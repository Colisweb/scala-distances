package com.colisweb.distances

import java.time.Instant

import cats.effect.Async
import com.colisweb.distances.Types._

abstract class DistanceProvider[F[_]: Async] {

  def distance(
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeDepartureTime: Option[Instant] = None
  ): F[Distance]
}

object DistanceProvider {
  type DistanceF[F[_]] = (TravelMode, LatLong, LatLong, Option[Instant]) => F[Distance]
}
