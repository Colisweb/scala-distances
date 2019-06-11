package com.colisweb.distances

import cats.effect.Async
import com.colisweb.distances.Types.{LatLong, _}

abstract class GeoProvider[F[_]: Async] {

  private[distances] def geocode(point: Point): F[LatLong]

}
