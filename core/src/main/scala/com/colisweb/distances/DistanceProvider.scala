package com.colisweb.distances

import cats.effect.Async
import com.colisweb.distances.Types._

abstract class DistanceProvider[F[_]: Async, E] {
  def distance(
      mode: TravelMode,
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Either[E, Distance]]

  def batchDistances(
      mode: TravelMode,
      origins: List[LatLong],
      destinations: List[LatLong],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[(LatLong, LatLong), Either[E, Distance]]]
}

object DistanceProvider {
  type DistanceF[F[_], E] = (TravelMode, LatLong, LatLong, Option[TrafficHandling]) => F[Either[E, Distance]]

  type BatchDistanceF[F[_], E] =
    (
        TravelMode,
        List[LatLong],
        List[LatLong],
        Option[TrafficHandling]
    ) => F[Map[(LatLong, LatLong), Either[E, Distance]]]
}
