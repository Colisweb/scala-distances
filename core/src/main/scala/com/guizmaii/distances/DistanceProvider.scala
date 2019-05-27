package com.guizmaii.distances

import java.time.Instant

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

abstract class DistanceProvider[F[_]: Async] {

  private[distances] def distance(mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance]

  private[distances] def distanceAtDepartureTime(mode: TravelMode, origin: LatLong, destination: LatLong, departure: Instant): F[Distance]

}
