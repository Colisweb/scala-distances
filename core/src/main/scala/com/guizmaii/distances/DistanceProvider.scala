package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

abstract class DistanceProvider[AIO[_]: Async] {

  private[distances] def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance]

}
