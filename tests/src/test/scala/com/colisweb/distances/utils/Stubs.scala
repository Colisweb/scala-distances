package com.colisweb.distances.utils

import java.time.Instant

import cats.Monad
import cats.effect.Async
import com.colisweb.distances.Types.{Distance, LatLong, TravelMode}
import com.colisweb.distances.{DistanceProvider, _}
import com.colisweb.distances.caches.NoCache
import squants.motion.KilometersPerHour
import squants.space.Meters

import scala.concurrent.duration._
import scala.math._

object Stubs {

  def distanceProviderStub[F[_]: Async]: DistanceProvider[F] = new DistanceProvider[F] {
    override def distance(
        mode: TravelMode,
        origin: LatLong,
        destination: LatLong,
        maybeDepartureTime: Option[Instant] = None
    ): F[Types.Distance] = ???
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

  def mockedDistanceF[F[_]: Monad](
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeDepartureTime: Option[Instant]
  ): F[Distance] = {
    Monad[F].pure {
      val distance       = Meters(haversine(origin, destination).round)
      val travelDuration = (distance / KilometersPerHour(50)).toSeconds.seconds
      val trafficDuration = mode match {
        case TravelMode.Driving => 5.minutes
        case _                  => 0.minute
      }

      maybeDepartureTime match {
        case Some(_) => Distance(distance, travelDuration + trafficDuration)
        case None    => Distance(distance, travelDuration)
      }
    }
  }

}
