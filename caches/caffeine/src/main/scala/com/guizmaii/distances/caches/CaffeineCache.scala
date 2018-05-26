package com.guizmaii.distances.caches

import cats.effect
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object CaffeineCache {

  import scalacache.caffeine.{CaffeineCache => InnerCaffeineCache}

  final def apply[AIO[_]: effect.Async](ttl: Option[Duration]): Cache[AIO] =
    new Cache[AIO](ttl) {
      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerCaffeineCache[Json]
    }

}
