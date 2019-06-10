package com.colisweb.distances.utils

import java.time.Instant

import cats.effect.Async
import com.colisweb.distances.DistanceProvider
import com.colisweb.distances.Types.TravelMode
import com.colisweb.distances.Types.LatLong
import com.colisweb.distances._
import com.colisweb.distances.caches.NoCache

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

}
