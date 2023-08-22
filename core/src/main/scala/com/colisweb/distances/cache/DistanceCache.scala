package com.colisweb.distances.cache

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.PathResult
import com.colisweb.simplecache.wrapper.cats.CatsCache
import org.slf4j.{Logger, LoggerFactory}

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

case class DistanceWithCache[F[_], P](
    cache: CatsCache[F, P, PathResult],
    api: DistanceApi[F, P]
)(implicit F: MonadError[F, Throwable])
    extends DistanceApi[F, P] {
  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)
  override def distance(path: P): F[PathResult] = {
    cache.get(path).attempt.flatMap {
      case Right(Some(distance)) => F.pure(distance)
      case Right(None)           => computeAndCacheDistance(path)
      case Left(error) =>
        F.pure(logger.warn(s"Fail to get distance from cache for $path", error)) *>
          computeAndCacheDistance(path)
    }
  }

  private def computeAndCacheDistance(path: P): F[PathResult] = {
    api
      .distance(path)
      .flatTap(
        cache
          .update(path, _)
          .handleError(logger.warn(s"Fail to get distance from cache for $path", _))
      )
  }
}
