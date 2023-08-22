package com.colisweb.distances.caches

import com.colisweb.simplecache.core.Cache
import com.colisweb.simplecache.redis.RedisConfiguration
import com.colisweb.simplecache.redis.RedisConfiguration.pool
import com.colisweb.simplecache.redis.codec._
import com.colisweb.simplecache.redis.circe.RedisCirceCache
import io.circe.Codec

import scala.concurrent.duration.FiniteDuration

object RedisCache {

  def apply[V](configuration: RedisConfiguration, ttl: Option[FiniteDuration])(implicit
      codec: Codec[V]
  ): Cache[Any, V] = {
    new RedisCirceCache[Any, V](pool(configuration), ttl)(
      keyEncoder = AnyEncoder(),
      codec
    )
  }
}
