package com.guizmaii.distances.utils

import cats.effect.Async
import com.guizmaii.distances.Types.LatLong
import com.guizmaii.distances._
import com.guizmaii.distances.caches.NoCache

object Stubs {

  def distanceProviderStub[F[_]](implicit async: Async[F]): DistanceProvider[F] =
    new DistanceProvider[F] {
      override def distance(mode: Types.TravelMode, origin: LatLong, destination: LatLong): F[Types.Distance] = ???

      override val F: Async[F] = async
    }

  def geoProviderStub[F[_]: Async]: GeoProvider[F] = new GeoProvider[F] {
    override def geocode(point: Types.Point): F[LatLong] = ???
  }

  def geocoderStub[F[_]: Async] = new Geocoder[F](geoProviderStub, NoCache())

}
