package com.guizmaii.distances.caches

import cats.effect
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object RedisCache {

  import scalacache.redis.{RedisCache => InnerRedisCache}

  final def apply[AIO[_]: effect.Async](config: RedisConfiguration, ttl: Option[Duration]): Cache[AIO] =
    new Cache[AIO](ttl) {
      import scalacache.serialization.circe._
      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerRedisCache[Json](config.jedisPool)
    }

}
