package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

abstract class DistanceProvider[F[_]: Async] {

  private[distances] def distance(mode: TravelMode, origin: LatLong, destination: LatLong): F[Distance]

}
