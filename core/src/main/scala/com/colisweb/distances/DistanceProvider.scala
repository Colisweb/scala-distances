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

  def multipleDistances(
      mode: TravelMode,
      origin: List[LatLong],
      destination: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[List[Distance]]
}

object DistanceProvider {
  type DistanceF[F[_]]          = (TravelMode, LatLong, LatLong, Option[TrafficHandling]) => F[Distance]
  type MultipleDistancesF[F[_]] = (TravelMode, List[LatLong], List[LatLong], Option[TrafficHandling]) => F[List[Distance]]
}
