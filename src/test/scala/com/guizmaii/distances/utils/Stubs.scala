package com.guizmaii.distances.utils

import cats.effect.Async
import com.guizmaii.distances.{DistanceProvider, GeoProvider, Geocoder, Types}
import com.guizmaii.distances.Types.LatLong

object Stubs {

  def distanceProviderStub[AIO[_]: Async]: DistanceProvider[AIO] = new DistanceProvider[AIO] {
    override def distance(mode: Types.TravelMode, origin: LatLong, destination: LatLong): AIO[Types.Distance] = ???
  }

  def geoProviderStub[AIO[_]: Async]: GeoProvider[AIO] = new GeoProvider[AIO] {
    override def geocode(point: Types.Point): AIO[LatLong] = ???
  }

  def geocoderStub[AIO[_]: Async] = new Geocoder[AIO](geoProviderStub)

}
