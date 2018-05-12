package com.guizmaii.distances.utils

import cats.effect.Async
import com.guizmaii.distances.Types.LatLong
import com.guizmaii.distances.providers.{CacheProvider, DistanceProvider, GeoProvider}
import com.guizmaii.distances.{Geocoder, Types}

object Stubs {

  def cacheProviderStub[AIO[_]: Async]: CacheProvider[AIO] = ???

  def distanceProviderStub[AIO[_]: Async]: DistanceProvider[AIO] = new DistanceProvider[AIO] {
    override def distance(mode: Types.TravelMode, origin: LatLong, destination: LatLong): AIO[Types.Distance] = ???
  }

  def geoProviderStub[AIO[_]: Async]: GeoProvider[AIO] = new GeoProvider[AIO] {
    override def geocode(point: Types.Point): AIO[LatLong] = ???
  }

  def geocoderStub[AIO[_]: Async] = new Geocoder[AIO](geoProviderStub, cacheProviderStub)

}
