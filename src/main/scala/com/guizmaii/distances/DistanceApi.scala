package com.guizmaii.distances

import com.guizmaii.distances.Types._
import monix.eval.Task

trait DistanceApi {

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance]): Task[Map[TravelMode, Distance]]

  def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): Task[Map[TravelMode, Distance]]

  def distances(paths: List[DirectedPath])(implicit cache: GeoCache[CacheableDistance]): Task[Map[(TravelMode, LatLong, LatLong), Distance]]

}
