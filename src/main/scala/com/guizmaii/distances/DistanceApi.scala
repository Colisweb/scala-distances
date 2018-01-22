package com.guizmaii.distances

import com.guizmaii.distances.Types._
import com.guizmaii.distances.utils.WithCache
import monix.eval.Task
import monix.execution.CancelableFuture

trait DistanceApi extends WithCache[((TravelMode, LatLong, LatLong), SerializableDistance)] {

  def distanceT(
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode] = List(TravelMode.Driving)
  ): Task[Map[TravelMode, Distance]]

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelMode: List[TravelMode] = List(TravelMode.Driving)
  ): CancelableFuture[Map[TravelMode, Distance]]

  def distanceFromPostalCodesT(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode] = List(TravelMode.Driving)
  ): Task[Map[TravelMode, Distance]]

  def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelMode: List[TravelMode] = List(TravelMode.Driving)
  ): CancelableFuture[Map[TravelMode, Distance]]

  def distancesT(paths: List[DirectedPath]): Task[Map[(TravelMode, LatLong, LatLong), Distance]]

  def distances(paths: List[DirectedPath]): CancelableFuture[Map[(TravelMode, LatLong, LatLong), Distance]]

}
