package com.guizmaii.distances

import com.guizmaii.distances.Types._
import com.guizmaii.distances.implementations.cache.GeoCache
import monix.eval.Task
import monix.execution.CancelableFuture

trait DistanceApi {

  def distanceT(
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance]): Task[Map[TravelMode, Distance]]

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance]): CancelableFuture[Map[TravelMode, Distance]]

  def distanceFromPostalCodesT(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): Task[Map[TravelMode, Distance]]

  def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode]
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): CancelableFuture[Map[TravelMode, Distance]]

  def distancesT(paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): Task[Map[(TravelMode, LatLong, LatLong), Distance]]

  def distances(paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): CancelableFuture[Map[(TravelMode, LatLong, LatLong), Distance]]

}
