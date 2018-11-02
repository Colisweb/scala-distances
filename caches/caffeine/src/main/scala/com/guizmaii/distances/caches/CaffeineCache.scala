package com.guizmaii.distances.caches

import cats.effect.Async
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object CaffeineCache {

  import scalacache.caffeine.{CaffeineCache => InnerCaffeineCache}

  final def apply[F[_]](ttl: Option[Duration])(implicit async: Async[F]): Cache[F] =
    new Cache[F] {

      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerCaffeineCache[Json]

      override val ttl0: Option[Duration] = ttl
      override implicit val F: Async[F]   = async
    }

}
