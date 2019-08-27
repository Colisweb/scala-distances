package com.colisweb.distances.utils

import cats.Monad
import cats.effect.Async
import com.colisweb.distances.Types.{Distance, LatLong, Segment, TrafficHandling}
import com.colisweb.distances.caches.NoCache
import com.colisweb.distances.{DistanceProvider, _}
import squants.motion.KilometersPerHour
import squants.space.Meters

import scala.concurrent.duration._
import scala.math._

object Stubs {
  def distanceProviderStub[F[_]: Async, E]: DistanceProvider[F, E] =
    new DistanceProvider[F, E] {
      override def distance(
          mode: TravelMode,
          origin: LatLong,
          destination: LatLong,
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Either[E, Distance]] = ???

      override def batchDistances(
          mode: TravelMode,
          origins: List[LatLong],
          destinations: List[LatLong],
          maybeTrafficHandling: Option[TrafficHandling] = None
      ): F[Map[Segment, Either[E, Distance]]] = ???
    }

  def geoProviderStub[F[_]: Async]: GeoProvider[F] = new GeoProvider[F] {
    override def geocode(point: Types.Point): F[LatLong] = ???
  }

  def geocoderStub[F[_]: Async] = new Geocoder[F](geoProviderStub, NoCache())

  private def haversine(origin: LatLong, destination: LatLong): Double = {
    val deltaLat = toRadians(destination.latitude - origin.latitude)
    val deltaLon = toRadians(destination.longitude - origin.longitude)

    val hav = pow(sin(deltaLat / 2), 2) + cos(toRadians(origin.latitude)) * cos(
      toRadians(destination.latitude)
    ) * pow(sin(deltaLon / 2), 2)
    val greatCircleDistance = 2 * atan2(sqrt(hav), sqrt(1 - hav))

    val earthRadiusMiles  = 3958.761
    val earthRadiusMeters = earthRadiusMiles / 0.00062137

    earthRadiusMeters * greatCircleDistance
  }

  def mockedBatchDistanceF[F[_]: Monad](
      mode: TravelMode,
      origins: List[LatLong],
      destinations: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Map[Segment, Either[Unit, Distance]]] =
    Monad[F].pure {
      origins
        .flatMap(origin => destinations.map(Segment(origin, _)))
        .map { segment =>
          val either: Either[Unit, Distance] =
            Right[Unit, Distance](mockDistance(mode, segment.origin, segment.destination, maybeTrafficHandling))

          segment -> either
        }
        .toMap
    }

  def mockedDistanceF[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[Either[Unit, Distance]] =
    Monad[F].pure(Right(mockDistance(mode, origin, destination, maybeTrafficHandling)))

  private def mockDistance[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): Distance = {
    val distance       = Meters(haversine(origin, destination).round)
    val travelDuration = (distance / KilometersPerHour(50)).toSeconds.seconds

    val trafficDuration =
      maybeTrafficHandling match {
        case Some(TrafficHandling(_, trafficModel)) =>
          mode match {
            case TravelMode.Driving =>
              trafficModel match {
                case TrafficModel.BestGuess   => 5.minutes
                case TrafficModel.Optimistic  => 2.minutes
                case TrafficModel.Pessimistic => 10.minutes
              }

            case _ => 0.minute
          }

        case None => 0.minute
      }

    Distance(distance, travelDuration + trafficDuration)
  }
}
