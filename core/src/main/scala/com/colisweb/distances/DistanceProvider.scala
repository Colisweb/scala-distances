package com.colisweb.distances

import cats.effect.Async
import com.colisweb.distances.Types._

abstract class DistanceProvider[F[_]: Async] {

  def distance(
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Distance]
}

object DistanceProvider {
  type DistanceF[F[_]] = (TravelMode, LatLong, LatLong, Option[TrafficHandling]) => F[Distance]
}
