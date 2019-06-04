package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Cache.CachingF
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

abstract class Cache[F[_]: Async](ttl: Option[Duration]) {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  private[distances] val innerCache: InnerCache[Json]

  // TODO Jules: An optimization is possible when there's a cache miss because in that case the deserialization is useless.
  def cachingF[V]: CachingF[F, V] =
    new CachingF[F, V] {
      override def apply(keys: Any*)(f: F[V])(implicit decoder: Decoder[V], encoder: Encoder[V]): F[V] =
        innerCache
          .cachingF(keys: _*)(ttl)(f.map(encoder.apply))
          .flatMap(json => Async[F].fromEither(decoder.decodeJson(json)))
    }
}

object Cache {
  trait Function2I2[F[_], V, I1, I2] {
    def apply(keys: Any*)(f: F[V])(implicit i1: I1, i2: I2): F[V]
  }

  type CachingF[F[_], V] = Function2I2[F, V, Decoder[V], Encoder[V]]
}
