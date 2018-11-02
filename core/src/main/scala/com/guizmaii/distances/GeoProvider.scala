package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

abstract class GeoProvider[F[_]: Async] {

  private[distances] def geocode(point: Point): F[LatLong]

}
