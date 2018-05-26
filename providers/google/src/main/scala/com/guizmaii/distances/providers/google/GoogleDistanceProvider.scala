package com.guizmaii.distances.providers.google

import cats.effect.Async
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.TravelMode._
import com.google.maps.model.{DistanceMatrixElement, LatLng => GoogleLatLng, TravelMode => GoogleTravelMode, Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types.TravelMode.{Bicycling, Driving, Unknown}
import com.guizmaii.distances.Types.{Distance, LatLong, TravelMode}
import com.guizmaii.distances.providers.DistanceProvider

import scala.concurrent.duration._
import scala.language.postfixOps

object GoogleDistanceProvider {

  import cats.implicits._
  import com.guizmaii.distances.providers.google.utils.Implicits._
  import squants.space.LengthConversions._

  final def apply[AIO[_]](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): DistanceProvider[AIO] = new DistanceProvider[AIO] {

    override private[distances] final def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(asGoogleTravelMode(mode))
        .origins(asGoogleLatLng(origin))
        .destinations(asGoogleLatLng(destination))
        .units(GoogleDistanceUnit.METRIC)
        .asEffect
        .map(r => asDistance(r.rows.head.elements.head))

  }

  @inline
  private[this] final def asGoogleTravelMode(travelMode: TravelMode): GoogleTravelMode = travelMode match {
    case Driving   => DRIVING
    case Bicycling => BICYCLING
    case Unknown   => UNKNOWN
  }

  @inline
  private[this] final def asDistance(element: DistanceMatrixElement): Distance =
    Distance(length = element.distance.inMeters meters, duration = element.duration.inSeconds seconds)

  @inline
  private[this] final def asGoogleLatLng(latLong: LatLong): GoogleLatLng = new GoogleLatLng(latLong.latitude, latLong.longitude)

}
