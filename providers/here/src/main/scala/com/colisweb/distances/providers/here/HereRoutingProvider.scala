package com.colisweb.distances.providers.here

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, Point, TravelMode}
import com.colisweb.distances.providers.here.HereAdaptor._
import io.circe.Codec
import io.circe.jawn.decode
import net.logstash.logback.marker.Markers.append
import net.logstash.logback.marker.{LogstashMarker, Markers}
import org.slf4j.{Logger, LoggerFactory, Marker}

import java.time.Instant

class HereRoutingProvider[F[_]](hereRoutingContext: HereRoutingContext, executor: RequestExecutor[F])(
    routingMode: RoutingMode
)(implicit F: MonadError[F, Throwable]) {
  import HereRoutingProvider._
  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)
  private val baseUrl             = "https://router.hereapi.com/v8/routes"

  def singleRequest(
      origin: Point,
      destination: Point,
      departure: Option[Instant],
      travelMode: TravelMode
  ): F[DistanceAndDuration] = {
    val paramsNoAuthent: Map[String, String] = Map(
      "origin"        -> s"${origin.latitude},${origin.longitude}",
      "destination"   -> s"${destination.latitude},${destination.longitude}",
      "departureTime" -> departure.map(_.toString).getOrElse("any"),
      "return"        -> "summary",
      "routingMode"   -> "fast",
      "alternatives"  -> "3"
    ) ++ travelMode.asHere

    val paramsWithAuthent: Map[String, String] = paramsNoAuthent + ("apiKey" -> hereRoutingContext.apiKey.value)

    for {
      _ <- F.pure(logger.debug(paramsNoAuthent.toMarkers, s"--> GET $baseUrl"))
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

      _ <- F.pure(logger.debug(Map("body" -> response.text()).toMarkers, s"<-- ${response.statusCode} $baseUrl"))
      result <- response match {
        case res if res.is2xx =>
          decode[Response](res.text()) match {
            case Left(err) => F.raiseError(UnknownHereResponse(err.getMessage))
            case Right(r) =>
              val results = r.routes.map(r =>
                DistanceAndDuration(r.sections.map(_.summary.length).sum / 1000, r.sections.map(_.summary.duration).sum)
              )
              if (results.nonEmpty)
                F.pure(routingMode.best(results))
              else
                F.raiseError(NoRouteFoundError(origin, destination))
          }
        case res if res.statusCode == 400 => F.raiseError(MalformedRequest(res.statusMessage))
        case res if res.statusCode == 401 => F.raiseError(UnauthorizedRequest(res.statusMessage))
        case res if res.statusCode == 403 => F.raiseError(NonAllowedRequest(res.statusMessage))
        case res if res.statusCode == 429 => F.raiseError(TooManyRequestError)
        case err                          => F.raiseError(UnknownHereError(err.statusMessage))
      }
    } yield result

  }

}

private object HereRoutingProvider {
  import io.circe.generic.extras.semiauto.deriveConfiguredCodec
  import io.circe.generic.extras.defaults._

  case class Response(routes: List[Route])
  case class Route(sections: List[Section])
  case class Section(summary: Summary)
  case class Summary(duration: Long, length: Double)

  implicit val responseCodec: Codec[Response] = deriveConfiguredCodec
  implicit val routeCodec: Codec[Route]       = deriveConfiguredCodec
  implicit val sectionCodec: Codec[Section]   = deriveConfiguredCodec
  implicit val summaryCodec: Codec[Summary]   = deriveConfiguredCodec

  implicit class MapToMarkers(params: Map[String, String]) {
    def toMarkers: Marker = params.foldLeft(Markers.empty()) { (marker, pair) =>
      marker.and[LogstashMarker](append(pair._1, pair._2))
    }
  }
}
