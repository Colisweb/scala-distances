package com.guizmaii.distances.providers

import java.util.concurrent.TimeUnit

import cats.effect.Async
import com.google.maps.model.TravelMode._
import com.google.maps.model.{DistanceMatrixElement, TravelMode => GoogleTravelMode, Unit => GoogleDistanceUnit}
import com.google.maps.{DistanceMatrixApi, GeoApiContext}
import com.guizmaii.distances.Types.TravelMode.{Bicycling, Driving, Unknown}
import com.guizmaii.distances.Types.{LatLong, _}

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

abstract class DistanceProvider[AIO[_]: Async] {

  private[distances] def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance]

}

object GoogleDistanceProvider {

  import squants.space.LengthConversions._

  final case class GoogleGeoApiContext(googleApiKey: String, connectTimeout: Duration, readTimeout: Duration) {

    /**
      * More infos about the rate limit:
      *   - https://developers.google.com/maps/documentation/distance-matrix/usage-limits
      */
    final val geoApiContext: GeoApiContext =
      new GeoApiContext.Builder()
        .apiKey(googleApiKey)
        .connectTimeout(connectTimeout.toMillis, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout.toMillis, TimeUnit.MILLISECONDS)
        .queryRateLimit(100)
        .build()

  }

  object GoogleGeoApiContext {
    final def apply(googleApiKey: String): GoogleGeoApiContext = new GoogleGeoApiContext(googleApiKey, 1 second, 1 second)
  }

  import cats.implicits._
  import com.guizmaii.distances.utils.RichImplicits._

  final def apply[AIO[_]](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): DistanceProvider[AIO] = new DistanceProvider[AIO] {

    override private[distances] final def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(asGoogleTravelMode(mode))
        .origins(origin.asGoogleLatLng)
        .destinations(destination.asGoogleLatLng)
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
  private final def asDistance(element: DistanceMatrixElement): Distance =
    Distance(length = element.distance.inMeters meters, duration = element.duration.inSeconds seconds)
}
