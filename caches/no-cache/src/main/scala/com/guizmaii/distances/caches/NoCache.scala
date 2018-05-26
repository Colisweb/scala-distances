package com.guizmaii.distances.caches

import cats.effect
import com.guizmaii.distances.Cache
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

object NoCache {

  final def apply[AIO[_]: effect.Async](): Cache[AIO] =
    new Cache[AIO](None) {
      override private[distances] implicit final val innerCache: InnerCache[Json] = null

      @inline
      override private[distances] def cachingF[V](keyParts: Any*)(f: => AIO[V])(
          implicit decoder: Decoder[V],
          encoder: Encoder[V]
      ): AIO[V] = f
    }

}
