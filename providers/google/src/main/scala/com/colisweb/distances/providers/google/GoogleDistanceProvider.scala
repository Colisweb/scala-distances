package com.colisweb.distances.providers.google

import java.time.{Instant, LocalDateTime, ZoneOffset}

import cats.effect.{Concurrent, Sync}
import com.colisweb.distances.TrafficModel._
import com.colisweb.distances.Types.LatLong._
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types.{Distance, LatLong, TrafficHandling}
import com.colisweb.distances.{DistanceProvider, TravelMode}
import com.google.maps.model.DistanceMatrixElementStatus._
import com.google.maps.model.{DistanceMatrix, DistanceMatrixElementStatus, Unit => GoogleDistanceUnit}
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NoStackTrace

sealed abstract class GoogleDistanceProviderError(message: String) extends RuntimeException(message) with NoStackTrace
final case class DistanceNotFound(message: String)                 extends GoogleDistanceProviderError(message)
final case class PastTraffic(message: String)                      extends GoogleDistanceProviderError(message)

object GoogleDistanceProvider {

  import cats.implicits._
  import com.colisweb.distances.providers.google.utils.Implicits._
  import squants.space.LengthConversions._

  final def apply[F[_]: Concurrent](geoApiContext: GoogleGeoApiContext): DistanceProvider[F] =
    new DistanceProvider[F] {

      def buildGoogleRequest(mode: TravelMode, origin: LatLong, destination: LatLong): DistanceMatrixApiRequest =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(mode.asGoogle)
          .origins(origin.asGoogle) // TODO: Multiple origins?
          .destinations(destination.asGoogle)
          .units(GoogleDistanceUnit.METRIC)

      def requestWithTraffic(request: DistanceMatrixApiRequest)(
          trafficHandling: TrafficHandling
      ): DistanceMatrixApiRequest =
        request
          .departureTime(trafficHandling.departureTime)
          .trafficModel(trafficHandling.trafficModel.asGoogle)

      def handleGoogleResponse(response: F[DistanceMatrix], mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance] =
        response
          .flatMap { response =>
            getDistance(response) match {
              case Some((OK, distance)) => distance.pure[F]
              case Some((NOT_FOUND, _)) =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                       |
                         | Indication from Google API code doc: "Indicates that the origin and/or destination of this pairing could not be geocoded."
                      """.stripMargin
                  )
                }
              case Some((ZERO_RESULTS, _)) =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API have zero results for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                       |
                         | Indication from Google API code doc: "Indicates that no route could be found between the origin and destination."
                      """.stripMargin
                  )
                }
              case None =>
                Sync[F].raiseError {
                  DistanceNotFound(
                    s"""
                       | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                      """.stripMargin
                  )
                }
            }
          }

      /**
        * Call the Google Maps API with the following arguments.
        * /!\ Using the maybeTrafficHandling argument results in a twice higher API call cost & traffic taken into account.
        *
        * @param mode Transportation mode (driving, bicycle...)
        * @param origin Origin point
        * @param destination Destination point
        * @param maybeTrafficHandling The traffic parameters, which are the departure time and the traffic estimation model.
        *                             If defined, this makes Google Maps take the traffic into account.
        * @return An Async typeclass instance of [[Distance]]
        */
      override final def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Distance] = {
        maybeTrafficHandling match {
          case Some(TrafficHandling(departureTime, trafficModel)) if departureTime.isBefore(Instant.now()) =>
            Sync[F].raiseError {
              PastTraffic(
                s"""
                   | Google Distance API does not handle past traffic requests.
                   | At ${LocalDateTime
                     .ofInstant(departureTime, ZoneOffset.UTC)} with model $trafficModel from ${origin.show} to ${destination.show}.
                 """.stripMargin
              )
            }

          case _ =>
            val baseRequest        = buildGoogleRequest(mode, origin, destination)
            val googleFinalRequest = maybeTrafficHandling.fold(baseRequest)(requestWithTraffic(baseRequest)).asEffect[F]

            handleGoogleResponse(googleFinalRequest, mode, origin, destination)
        }
      }
    }

  @inline
  private[this] final def getDistance(distanceMatrix: DistanceMatrix): Option[(DistanceMatrixElementStatus, Distance)] =
    for {
      row     <- distanceMatrix.rows.headOption
      element <- row.elements.headOption
    } yield
      element.status match {
        case OK =>
          val durationInTraffic = Option(element.durationInTraffic).map(_.inSeconds seconds).getOrElse(0 seconds)
          val travelDuration    = element.duration.inSeconds seconds

          OK -> Distance(
            length = element.distance.inMeters meters,
            duration = travelDuration.plus(durationInTraffic)
          )

        case e => e -> null // Very bad !
      }
}
