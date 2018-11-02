package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}
import simulacrum.typeclass

@typeclass
trait DistanceProvider[F[_]] {

  def F: Async[F]

  private[distances] def distance(mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance]

}
