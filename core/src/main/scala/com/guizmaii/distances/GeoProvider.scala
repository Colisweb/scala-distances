package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

abstract class GeoProvider[AIO[_]: Async] {

  private[distances] def geocode(point: Point): AIO[LatLong]

}
