package com.colisweb.distances

import cats.effect.Async
import io.circe.{Decoder, Encoder, Json}
import scalacache.{Cache => InnerCache}

import scala.concurrent.duration.Duration

abstract class Cache[F[_]: Async](ttl: Option[Duration]) {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  private[distances] val innerCache: InnerCache[Json]

  // TODO Jules: An optimization is possible when there's a cache miss because in that case the deserialization is useless.
  def cachingF[V](f: F[V], decoder: Decoder[V], encoder: Encoder[V], keys: Any*): F[V] =
    innerCache
      .cachingF(keys: _*)(ttl)(f.map(encoder.apply))
      .flatMap(json => Async[F].fromEither(decoder.decodeJson(json)))

  def caching[V](v: V, decoder: Decoder[V], encoder: Encoder[V], keys: Any*): F[V] =
    innerCache
      .caching(keys: _*)(ttl)(encoder.apply(v))
      .flatMap(json => Async[F].fromEither(decoder.decodeJson(json)))

  def get[V](decoder: Decoder[V], keys: Any*): F[Option[V]] =
    innerCache
      .get(keys)
      .flatMap(_.traverse(json => Async[F].fromEither(decoder.decodeJson(json))))
}

object Cache {

  // V is the cached value
  // Any* corresponds to the key parts of V see [[InnerCache.cachingF()]]
  type CachingF[F[_], V] = (F[V], Decoder[V], Encoder[V], Any*) => F[V]

  type Caching[F[_], V] = (V, Decoder[V], Encoder[V], Any*) => F[V]

  type GetCached[F[_], V] = (Decoder[V], Any*) => F[Option[V]]
}
