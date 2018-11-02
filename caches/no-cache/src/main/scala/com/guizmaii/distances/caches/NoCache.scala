package com.guizmaii.distances.caches

import cats.effect.Async
import com.guizmaii.distances.Cache
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

object NoCache {

  final def apply[F[_]]()(implicit async: Async[F]): Cache[F] =
    new Cache[F] {
      override private[distances] implicit final val innerCache: InnerCache[Json] = null

      @inline
      override private[distances] def cachingF[V](keyParts: Any*)(f: => F[V])(
          implicit decoder: Decoder[V],
          encoder: Encoder[V]
      ): F[V] = f

      override val ttl0: Option[Duration] = None
      override implicit val F: Async[F]   = async
    }

}
