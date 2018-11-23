package com.guizmaii.distances

import cats.effect.Async
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

abstract class Cache[F[_]: Async](ttl: Option[Duration]) {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  private[distances] val innerCache: InnerCache[Json]

  // TODO Jules: An optimization is possible when there's a cache miss because in that case the deserialization is useless.
  private[distances] def cachingF[V](keyParts: Any*)(f: => F[V])(implicit decoder: Decoder[V], encoder: Encoder[V]): F[V] =
    innerCache
      .cachingF(keyParts: _*)(ttl)(f.map(encoder.apply))
      .flatMap(json => Async[F].fromEither(decoder.decodeJson(json)))

}
