package com.guizmaii.distances

import cats.effect.Async
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.{Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types.{LatLong, _}
import com.guizmaii.distances.utils.GoogleGeoApiContext

abstract class DistanceProvider[AIO[_]: Async] {

  def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance]

}

object GoogleDistanceProvider {

  import TravelMode._
  import cats.implicits._
  import com.guizmaii.distances.utils.RichImplicits._

  def apply[AIO[_]](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): DistanceProvider[AIO] = new DistanceProvider[AIO] {

    override def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(mode.asGoogleTravelMode)
        .origins(origin.asGoogleLatLng)
        .destinations(destination.asGoogleLatLng)
        .units(GoogleDistanceUnit.METRIC)
        .asEffect
        .map(_.rows.head.elements.head.asDistance)

  }
}
