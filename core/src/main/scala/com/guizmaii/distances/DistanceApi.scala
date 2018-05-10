package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types._

trait DistanceApi {

  def distance[AIO[_]: Async](
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance]): AIO[Map[TravelMode, Distance]]

  def distanceFromPostalCodes[AIO[_]: Async](geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): AIO[Map[TravelMode, Distance]]

  def distances[AIO[_]: Async](paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]]

}
