package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, _}

import scala.util.control.NoStackTrace

sealed abstract class GeoProviderError(message: String) extends RuntimeException(message) with NoStackTrace
final case class PointNotFound(message: String)         extends GeoProviderError(message)

abstract class GeoProvider[F[_]: Async] {

  private[distances] def geocode(point: Point): F[LatLong]

}
