package com.colisweb.distances.caches

import cats.effect.Async
import com.colisweb.distances.Cache
import io.circe.Json
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object CaffeineCache {

  import scalacache.caffeine.{CaffeineCache => InnerCaffeineCache}

  final def apply[F[_]: Async](ttl: Option[Duration]): Cache[F] =
    new Cache[F](ttl) {
      override private[distances] final val innerCache: InnerCache[Json] = InnerCaffeineCache[Json]
    }

}
