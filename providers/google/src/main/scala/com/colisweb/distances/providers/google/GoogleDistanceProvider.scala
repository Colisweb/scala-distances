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

      def buildGoogleRequest(mode: TravelMode, origins: List[LatLong], destinations: List[LatLong]): DistanceMatrixApiRequest =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(mode.asGoogle)
          .origins(origins.map(_.asGoogle): _*)
          .destinations(destinations.map(_.asGoogle): _*)
          .units(GoogleDistanceUnit.METRIC)

      def requestWithTraffic(request: DistanceMatrixApiRequest)(
          trafficHandling: TrafficHandling
      ): DistanceMatrixApiRequest =
        request
          .departureTime(trafficHandling.departureTime)
          .trafficModel(trafficHandling.trafficModel.asGoogle)

      def handleGoogleResponse(
          response: F[DistanceMatrix],
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong]
      ): F[List[Distance]] =
        response.flatMap { response =>
          val orderedPairs = origins.flatMap(origin => destinations.map(origin -> _))

          getDistances(response)
            .zip(orderedPairs)
            .map {
              case ((status, maybeDistance), (origin, destination)) =>
                (status, maybeDistance) match {
                  case (OK, Some(distance)) => distance.pure[F]
                  case (NOT_FOUND, _) =>
                    Sync[F].raiseError[Distance] {
                      DistanceNotFound(
                        s"""
                           | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                           |
                           | Indication from Google API code doc: "Indicates that the origin and/or destination of this pairing could not be geocoded."
                      """.stripMargin
                      )
                    }

                  case (ZERO_RESULTS, _) =>
                    Sync[F].raiseError[Distance] {
                      DistanceNotFound(
                        s"""
                           | Google Distance API have zero results for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                           |
                           | Indication from Google API code doc: "Indicates that no route could be found between the origin and destination."
                      """.stripMargin
                      )
                    }

                  case (_, None) =>
                    Sync[F].raiseError[Distance] {
                      DistanceNotFound(
                        s"""
                           | Google Distance API didn't find the distance for ${origin.show} to ${destination.show} with "${mode.show}" travel mode.
                      """.stripMargin
                      )
                    }
                }
            }
        }

      override final def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Distance] =
        multipleDistances(mode, List(origin), List(destination), maybeTrafficHandling).map(_.head)

      /**
        * Call the Google Maps API with the following arguments.
        * /!\ Using the maybeTrafficHandling argument results in a twice higher API call cost & traffic taken into account.
        *
        * @param mode Transportation mode (driving, bicycle...)
        * @param origins Origin points
        * @param destinations Destination points
        * @param maybeTrafficHandling The traffic parameters, which are the departure time and the traffic estimation model.
        *                             If defined, this makes Google Maps take the traffic into account.
        * @return An Async typeclass instance of [[Distance]]
        */
      override final def multipleDistances(
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong],
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): List[F[Distance]] = {
        maybeTrafficHandling match {
          case Some(TrafficHandling(departureTime, trafficModel)) if departureTime.isBefore(Instant.now()) =>
            Sync[F].raiseError {
              PastTraffic(
                s"""
                   | Google Distance API does not handle past traffic requests.
                   | At ${LocalDateTime.ofInstant(departureTime, ZoneOffset.UTC)} with model $trafficModel from ${origins.show} to ${destinations.show}.
                 """.stripMargin
              )
            }

          case _ =>
            val baseRequest        = buildGoogleRequest(mode, origins, destinations)
            val googleFinalRequest = maybeTrafficHandling.fold(baseRequest)(requestWithTraffic(baseRequest)).asEffect[F]

            handleGoogleResponse(googleFinalRequest, mode, origins, destinations).map { response =>
              response.map {
                case Right(distance) => distance.pure[F]
                case Left(error) => Sync[F].raiseError(error)
              }
            }
        }
      }
    }

  @inline
  private[this] final def getDistances(distanceMatrix: DistanceMatrix): List[(DistanceMatrixElementStatus, Option[Distance])] =
    (for {
      row     <- distanceMatrix.rows
      element <- row.elements
    } yield {
      element.status match {
        case OK =>
          val durationInTraffic = Option(element.durationInTraffic).map(_.inSeconds seconds).getOrElse(0 seconds)
          val travelDuration    = element.duration.inSeconds seconds

          OK -> Some(
            Distance(
              length = element.distance.inMeters meters,
              duration = travelDuration.plus(durationInTraffic)
            ))

        case e => e -> None
      }
    }).toList
}
