package com.colisweb.distances.providers.here

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{DistanceAndDuration, PathResult, Point, TravelMode}
import com.colisweb.distances.providers.here.HereAdaptor._
import com.colisweb.distances.providers.here.polyline.PolylineEncoderDecoder
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.jawn.decode
import net.logstash.logback.marker.Markers.append
import net.logstash.logback.marker.{LogstashMarker, Markers}
import org.slf4j.{Logger, LoggerFactory, Marker}
import requests.{Response => RResponse}

import java.time.Instant
import scala.jdk.CollectionConverters._

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
      travelMode: TravelMode,
      segments: Int = 1
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
            case Left(err) => F.raiseError(UnknownHereResponse(err.getMessage))
            case Right(r) =>
              if (r.routes.nonEmpty) {
                val bestRoute    = routingMode.best(r.routes)
                val roadSegments = parseRoadSegments(origin, destination, bestRoute, segments)
                F.pure(PathResult(bestRoute.durationAndDistance, roadSegments))
              } else {
                F.raiseError(NoRouteFoundError(origin, destination))
              }
          }
        case res if res.statusCode == 400 => F.raiseError(MalformedRequest(res.statusMessage))
        case res if res.statusCode == 401 => F.raiseError(UnauthorizedRequest(res.statusMessage))
        case res if res.statusCode == 403 => F.raiseError(NonAllowedRequest(res.statusMessage))
        case res if res.statusCode == 429 => F.raiseError(TooManyRequestError)
        case err                          => F.raiseError(UnknownHereError(err.statusMessage))
      }
    } yield result

  }

  def logOnResponse(response: RResponse): (Marker, String) => Unit =
    if (response.is2xx)
      logger.debug
    else
      logger.warn

  private def parseRoadSegments(
      origin: Point,
      destination: Point,
      bestRoute: Route,
      segments: Int
  ): List[DirectedPath] = {
    // in routes, sections is a list but there was always only one element in it so far
    val polyline: List[Point] =
      bestRoute.sections.flatMap(section => PolylineEncoderDecoder.decode(section.polyline).asScala.toList.map(toPoint))

    val polylineWithOriginDestination =
      polyline.updated(0, origin).updated(polyline.size - 1, destination)

    reducePolyline(polylineWithOriginDestination, segments)
      .sliding(2)
      .toList
      .flatMap {
        case List(from, to) => Some(DirectedPath(from, to))
        case _              => None
      }
  }

  private def reducePolyline(polyline: List[Point], segments: Int): List[Point] =
    if (segments >= polyline.size)
      polyline
    else if (polyline.isEmpty)
      Nil
    else if (segments == 1)
      List(polyline.head, polyline.last)
    else
      (0 to segments).toList.flatMap(i => polyline.lift(i * polyline.size / segments))
}

private[here] object HereRoutingProvider {
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec

  case class Response(routes: List[Route])
  case class Route(sections: List[Section]) {
    val durationAndDistance: DistanceAndDuration =
      DistanceAndDuration(sections.map(_.summary.length).sum / 1000, sections.map(_.summary.duration).sum)
  }
  case class Section(summary: Summary, polyline: String)
  case class Summary(duration: Long, length: Double)

  implicit val codecConfig: Configuration = Configuration.default

  implicit val responseCodec: Codec[Response] = deriveConfiguredCodec
  implicit val routeCodec: Codec[Route]       = deriveConfiguredCodec
  implicit val sectionCodec: Codec[Section]   = deriveConfiguredCodec
  implicit val summaryCodec: Codec[Summary]   = deriveConfiguredCodec

  def toPoint(herePoint: PolylineEncoderDecoder.LatLngZ): Point =
    Point(herePoint.lat, herePoint.lng, Some(herePoint.z))

  implicit class MapToMarkers(params: Map[String, String]) {
    def toMarkers: Marker = params.foldLeft(Markers.empty()) { (marker, pair) =>
      marker.and[LogstashMarker](append(pair._1, pair._2))
    }
  }
}
