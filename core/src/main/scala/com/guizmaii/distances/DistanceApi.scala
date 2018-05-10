package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types._

abstract class DistanceApi[AIO[_]: Async] {

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]]

  def distanceFromPostalCodes(geocoder: Geocoder[AIO])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]]

  def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]]

}
