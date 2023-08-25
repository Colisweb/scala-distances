package com.colisweb.distances.cache

import cats.MonadError
import cats.effect.Sync
import cats.implicits._
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.PathResult
import com.colisweb.simplecache.wrapper.cats._

import scala.util.control.NoStackTrace

case class DistanceFromCache[F[_], P](
    cache: CatsCache[F, P, PathResult]
)(implicit F: MonadError[F, Throwable])
    extends DistanceApi[F, P] {

  override def distance(path: P): F[PathResult] =
    cache.get(path).flatMap {
      case Some(value) => F.pure(value)
      case None        => F.raiseError(CacheMissError(path))
    }
}

final case class CacheMissError(key: Any) extends RuntimeException(s"No entry in cache for $key") with NoStackTrace

case class DistanceWithCache[F[_]: Sync, P](
    cache: CatsCache[F, P, PathResult],
    api: DistanceApi[F, P]
) extends DistanceApi[F, P] {
  override def distance(path: P): F[PathResult] = cache.getOrElseUpdate(path, api.distance(path))

}
