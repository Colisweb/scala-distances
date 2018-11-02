package com.guizmaii.distances.caches

import cats.effect
import com.guizmaii.distances.Cache
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

object NoCache {

  final def apply[F[_]: effect.Async](): Cache[F] =
    new Cache[F](None) {
      override private[distances] implicit final val innerCache: InnerCache[Json] = null

      @inline
      override private[distances] def cachingF[V](keyParts: Any*)(f: => F[V])(
          implicit decoder: Decoder[V],
          encoder: Encoder[V]
      ): F[V] = f
    }

}
