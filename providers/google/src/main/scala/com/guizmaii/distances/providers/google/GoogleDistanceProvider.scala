package com.guizmaii.distances.providers.google

import cats.effect.Async
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.TravelMode._
import com.google.maps.model.{DistanceMatrixElement, LatLng => GoogleLatLng, TravelMode => GoogleTravelMode, Unit => GoogleDistanceUnit}
import com.guizmaii.distances.DistanceProvider
import com.guizmaii.distances.Types.TravelMode._
import com.guizmaii.distances.Types.{Distance, LatLong, TravelMode}

import scala.concurrent.duration._
import scala.language.postfixOps

object GoogleDistanceProvider {

  import cats.implicits._
  import com.guizmaii.distances.providers.google.utils.Implicits._
  import squants.space.LengthConversions._

  final def apply[F[_]: Async](geoApiContext: GoogleGeoApiContext): DistanceProvider[F] =
    new DistanceProvider[F] {

      override private[distances] final def distance(mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance] =
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
  private[this] final def asGoogleTravelMode(travelMode: TravelMode): GoogleTravelMode =
    travelMode match {
      case Driving   => DRIVING
      case Bicycling => BICYCLING
      case Walking   => WALKING
      case Transit   => TRANSIT
      case Unknown   => UNKNOWN
    }

  @inline
  private[this] final def asDistance(element: DistanceMatrixElement): Distance =
    Distance(length = element.distance.inMeters meters, duration = element.duration.inSeconds seconds)

  @inline
  private[this] final def asGoogleLatLng(latLong: LatLong): GoogleLatLng = new GoogleLatLng(latLong.latitude, latLong.longitude)

}
