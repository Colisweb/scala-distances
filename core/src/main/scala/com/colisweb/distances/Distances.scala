package com.colisweb.distances

import cats.effect.Sync
import cats.{Applicative, MonadError}
import com.colisweb.distances.bird.HaversineDistanceApi
import com.colisweb.distances.cache.{DistanceFromCache, DistanceWithCache}
import com.colisweb.distances.correction.CorrectPastDepartureTime
import com.colisweb.distances.model._
import com.colisweb.simplecache.wrapper.cats.CatsCache

import java.time.Clock
import scala.concurrent.duration.FiniteDuration

case class Distances[F[_], P](api: DistanceApi[F, P]) {

  def fallback(other: DistanceApi[F, P])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other))

  def fallback(other: Distances[F, P])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other.api))

  def fallbackWhen(
      other: DistanceApi[F, P]
  )(when: PartialFunction[Throwable, F[Unit]])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other, when))

  def fallbackWhen(
      other: Distances[F, P]
  )(when: PartialFunction[Throwable, F[Unit]])(implicit F: MonadError[F, Throwable]): Distances[F, P] =
    copy(api = Fallback(api, other.api, when))

  def caching(cache: CatsCache[F, P, PathResult])(implicit F: Sync[F]): Distances[F, P] =
    copy(api = DistanceWithCache(cache, api))

  def correctPastDepartureTime(
      margin: FiniteDuration,
      clock: Clock = Clock.systemDefaultZone()
  )(implicit DT: DepartureTime[P], DTU: DepartureTimeUpdatable[P]): Distances[F, P] =
    copy(api = CorrectPastDepartureTime(api, margin, clock))
}

object Distances {

  def haversine[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]: Distances[F, P] =
    from(new HaversineDistanceApi[F, P])

  implicit def from[F[_], P](api: DistanceApi[F, P]): Distances[F, P] = Distances(api)

  implicit def fromCache[F[_], P](cache: CatsCache[F, P, PathResult])(implicit
      F: MonadError[F, Throwable]
  ): Distances[F, P] =
    from(DistanceFromCache(cache))
}
