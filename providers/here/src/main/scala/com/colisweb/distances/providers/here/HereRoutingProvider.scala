package com.colisweb.distances.providers.here

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model._
import com.colisweb.distances.providers.here.HereAdaptor._
import com.colisweb.distances.providers.here.polyline.PolylineDecoder
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.jawn.decode
import net.logstash.logback.marker.Markers.append
import net.logstash.logback.marker.{LogstashMarker, Markers}
import org.slf4j.{Logger, LoggerFactory, Marker}
import requests.{Response => RResponse}

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class HereRoutingProvider[F[_]](hereRoutingContext: HereRoutingContext, executor: RequestExecutor[F])(
    routingMode: RoutingMode,
    excludeCountriesIso: List[String] = Nil
)(implicit F: MonadError[F, Throwable]) {
  import HereRoutingProvider._

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)
  private val baseUrl             = "https://router.hereapi.com/v8/routes"

  def singleRequest(
      origin: Point,
      destination: Point,
      departure: Option[Instant],
      travelMode: TravelMode
  ): F[PathResult] = {
    val excludeCountriesParams =
      if (excludeCountriesIso.isEmpty)
        Map.empty
      else
        Map("exclude[countries]" -> excludeCountriesIso.mkString(","))

    val paramsNoAuthent: Map[String, String] = Map(
      "origin"          -> s"${origin.latitude},${origin.longitude}",
      "destination"     -> s"${destination.latitude},${destination.longitude}",
      "departureTime"   -> departure.map(_.toString).getOrElse("any"),
      "return"          -> "summary,polyline,elevation",
      "routingMode"     -> "fast",
      "alternatives"    -> "3",
      "avoid[features]" -> "ferry,carShuttleTrain,dirtRoad"
    ) ++ travelMode.asHere ++ excludeCountriesParams

    val paramsWithAuthent: Map[String, String] = paramsNoAuthent + ("apiKey" -> hereRoutingContext.apiKey.value)

    for {
      response <- executor.run(
        requests.get(
          url = baseUrl,
          headers = List(("Accept", "application/json")),
          params = paramsWithAuthent.toList,
          connectTimeout = hereRoutingContext.connectTimeout.toMillis.toInt,
          readTimeout = hereRoutingContext.readTimeout.toMillis.toInt,
          check = false
        )
      )
      _ <- F.pure(
        logOnResponse(response)(Map("body" -> response.text()).toMarkers, s"<-- ${response.statusCode} $baseUrl")
      )
      result <- response match {
        case res if res.is2xx =>
          decode[Response](res.text()) match {
            case Left(err)   => F.raiseError(UnknownHereResponse(err.getMessage))
            case Right(resp) => parseResult(origin, destination, resp)
          }
        case res if res.statusCode == 400 => F.raiseError(MalformedRequest(res.statusMessage))
        case res if res.statusCode == 401 => F.raiseError(UnauthorizedRequest(res.statusMessage))
        case res if res.statusCode == 403 => F.raiseError(NonAllowedRequest(res.statusMessage))
        case res if res.statusCode == 429 => F.raiseError(TooManyRequestError)
        case err                          => F.raiseError(UnknownHereError(err.statusMessage))
      }
    } yield result

  }

  private def parseResult(origin: Point, destination: Point, r: Response): F[PathResult] =
    if (r.routes.nonEmpty) {
      val bestRoute        = routingMode.best(r.routes)
      val roadSegments     = parseRoadSegments(origin, destination, bestRoute)
      val elevationProfile = computeElevationProfile(roadSegments, bestRoute.duration, bestRoute.distance)
      F.pure(PathResult(bestRoute.distance, bestRoute.duration, Some(elevationProfile)))
    } else {
      F.raiseError(NoRouteFoundError(origin, destination))
    }

  def logOnResponse(response: RResponse): (Marker, String) => Unit =
    if (response.is2xx)
      logger.debug
    else
      logger.warn

  private def parseRoadSegments(origin: Point, destination: Point, bestRoute: Route): List[DirectedPath] = {
    // in routes, sections is a list but there was always only one element in it so far
    // there will be multiple elements if we use the 'via' parameter or if we have multiple travel modes
    bestRoute.sections.flatMap { section =>
      Try(PolylineDecoder.decode(section.polyline)) match {
        case Failure(exception) =>
          logger.warn(
            s"Error while decoding Here polyline from $origin to $destination: ${exception.getMessage}",
            exception
          )
          Nil
        case Success(polylinePoints) =>
          buildSubPaths(polylinePoints.asScala.toList.map(toPoint), origin, destination)
      }
    }
  }

  private def buildSubPaths(polyline: List[Point], origin: Point, destination: Point): List[DirectedPath] = {
    val lastIndex = polyline.size - 1

    // sometimes Here adapt the origin and the destination in the response and in the polyline
    // so here we revert those changes in the polyline to keep our input coordinates
    // we also add the elevation as we didn't have it in the input parameters
    val polylineWithOriginDestination =
      polyline
        .updated(0, origin.copy(elevation = polyline(1).elevation))
        .updated(lastIndex, destination.copy(elevation = polyline(lastIndex - 1).elevation))

    polylineWithOriginDestination
      .sliding(2)
      .toList
      .flatMap {
        case List(from, to) => Some(DirectedPath(from, to))
        case _              => None
      }
  }

  private def computeElevationProfile(
      subPaths: List[DirectedPath],
      totalDuration: DurationInSeconds,
      totalDistance: DistanceInKm
  ): Double = {
    val averageSpeedInMS             = totalDistance * 1000 / totalDuration
    val rollingResistanceCoefficient = 0.0125

    subPaths.foldLeft(0d) { case (acc, path) =>
      val subPathTravelTimeInSeconds = path.birdDistanceInKm * 1000 / averageSpeedInMS
      val angle                      = path.elevationAngleInRadians
      acc + (subPathTravelTimeInSeconds * (math.sin(angle) + rollingResistanceCoefficient * math.cos(angle)))
    }
  }
}

private[here] object HereRoutingProvider {
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec

  case class Response(routes: List[Route])
  case class Route(sections: List[Section]) {
    val distance: DistanceInKm      = sections.map(_.summary.length).sum / 1000
    val duration: DurationInSeconds = sections.map(_.summary.duration).sum
  }
  case class Section(summary: Summary, polyline: String)
  case class Summary(duration: Long, length: Double)

  implicit val codecConfig: Configuration = Configuration.default

  implicit val responseCodec: Codec[Response] = deriveConfiguredCodec
  implicit val routeCodec: Codec[Route]       = deriveConfiguredCodec
  implicit val sectionCodec: Codec[Section]   = deriveConfiguredCodec
  implicit val summaryCodec: Codec[Summary]   = deriveConfiguredCodec

  def toPoint(herePoint: PolylineDecoder.LatLngZ): Point =
    Point(herePoint.lat, herePoint.lng, Some(herePoint.z))

  implicit class MapToMarkers(params: Map[String, String]) {
    def toMarkers: Marker = params.foldLeft(Markers.empty()) { (marker, pair) =>
      marker.and[LogstashMarker](append(pair._1, pair._2))
    }
  }
}
