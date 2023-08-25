package com.colisweb.distances.caches

import cats.effect.Sync
import com.colisweb.simplecache.redis.RedisConfiguration
import com.colisweb.simplecache.redis.RedisConfiguration.pool
import com.colisweb.simplecache.redis.codec._
import com.colisweb.simplecache.redis.circe.RedisCirceCache
import com.colisweb.simplecache.wrapper.cats.CatsCache
import io.circe.Codec

import scala.concurrent.duration.FiniteDuration

object CatsRedisCache {

  def apply[F[_]: Sync, K, V](
      configuration: RedisConfiguration,
      ttl: Option[FiniteDuration],
      keyEncoder: Encoder[K] = AnyEncoder()
  )(implicit codec: Codec[V]): CatsCache[F, K, V] =
    CatsCache(new RedisCirceCache[K, V](pool(configuration), ttl)(keyEncoder, codec))
}
