package com.guizmaii.distances

import cats.effect.Effect
import com.guizmaii.distances.Types._

trait DistanceApi {

  def distance[E[_]: Effect](
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance]): E[Map[TravelMode, Distance]]

  def distanceFromPostalCodes[E[_]: Effect](geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): E[Map[TravelMode, Distance]]

  def distances[E[_]: Effect](paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): E[Map[(TravelMode, LatLong, LatLong), Distance]]

}
