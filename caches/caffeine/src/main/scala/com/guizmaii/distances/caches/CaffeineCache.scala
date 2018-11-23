package com.guizmaii.distances.caches

import cats.effect.Async
import com.guizmaii.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object CaffeineCache {

  import scalacache.caffeine.{CaffeineCache => InnerCaffeineCache}

  final def apply[F[_]: Async](ttl: Option[Duration]): Cache[F] =
    new Cache[F](ttl) {
      override private[distances] implicit final val innerCache: InnerCache[Json] = InnerCaffeineCache[Json]
    }

}
