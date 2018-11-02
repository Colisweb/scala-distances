package com.guizmaii.distances.caches

import cats.effect
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object RedisCache {

  import scalacache.redis.{RedisCache => InnerRedisCache}

  final def apply[F[_]: effect.Async](config: RedisConfiguration, ttl: Option[Duration]): Cache[F] =
    new Cache[F](ttl) {
      import scalacache.serialization.circe._
      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerRedisCache[Json](config.jedisPool)
    }

}
