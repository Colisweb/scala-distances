package com.colisweb.distances.caches

import cats.effect.Sync
import com.colisweb.simplecache.memory.guava.GuavaCache
import com.colisweb.simplecache.wrapper.cats.CatsCache

import scala.concurrent.duration.FiniteDuration

object CatsGuavaCache {

  def apply[F[_]: Sync, K, V](ttl: Option[FiniteDuration]): CatsCache[F, K, V] =
    CatsCache(new GuavaCache[K, V](ttl))
}
