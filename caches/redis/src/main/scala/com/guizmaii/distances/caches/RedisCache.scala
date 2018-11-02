package com.guizmaii.distances.caches

import cats.effect
import cats.effect.Async
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object RedisCache {

  import scalacache.redis.{RedisCache => InnerRedisCache}

  final def apply[F[_]: effect.Async](config: RedisConfiguration, ttl: Option[Duration])(implicit async: Async[F]): Cache[F] =
    new Cache[F] {
      import scalacache.serialization.circe._

      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerRedisCache[Json](config.jedisPool)

      override val ttl0: Option[Duration] = ttl
      override implicit val F: Async[F]   = async
    }

}
