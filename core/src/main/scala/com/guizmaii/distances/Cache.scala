package com.guizmaii.distances

import cats.effect.Async
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}
import simulacrum.typeclass

import scala.concurrent.duration.Duration

@typeclass
trait Cache[F[_]] {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  def ttl0: Option[Duration]
  implicit def F: Async[F]

  private[distances] implicit def innerCache: InnerCache[Json]

  // TODO Jules: An optimization is possible when there's a cache miss because in that case the deserialization is useless.
  private[distances] def cachingF[V](keyParts: Any*)(f: => F[V])(
      implicit decoder: Decoder[V],
      encoder: Encoder[V]
  ): F[V] =
    innerCache
      .cachingF(keyParts: _*)(ttl0)(f.map(encoder.apply))
      .flatMap(json => F.fromEither(decoder.decodeJson(json)))

}
