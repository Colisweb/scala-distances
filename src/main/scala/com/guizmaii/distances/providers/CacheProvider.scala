package com.guizmaii.distances.providers

import cats.effect
import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, TravelMode}
import io.circe.{Decoder, Encoder, Json}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool
import scalacache.Cache

import scala.concurrent.duration.Duration

abstract class CacheProvider[AIO[_]](ttl: Option[Duration])(implicit AIO: Async[AIO]) {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  // TODO: Should be private but I need to access it in tests
  implicit val innerCache: Cache[Json]

  final def cachingF[V](mode: TravelMode, origin: LatLong, destination: LatLong)(f: => AIO[V])(
      implicit decoder: Decoder[V],
      encoder: Encoder[V]
  ): AIO[V] =
    innerCache
      .cachingF((mode, origin, destination))(ttl)(f.map(encoder.apply))
      .flatMap(json => AIO.fromEither(decoder.decodeJson(json)))

}

object InMemoryCacheProvider {

  import scalacache._
  import scalacache.caffeine._

  final def apply[AIO[_]: effect.Async](ttl: Option[Duration]): CacheProvider[AIO] =
    new CacheProvider[AIO](ttl) {
      override implicit final val innerCache: Cache[Json] = CaffeineCache[Json]
    }

}

object RedisCacheProvider {

  import scalacache._
  import scalacache.redis._

  final case class RedisConfiuration(jedisPool: JedisPool) extends AnyVal

  object RedisConfiuration {
    final def apply(host: String, port: Int): RedisConfiuration = RedisConfiuration(new JedisPool(host, port))

    final def apply(host: String, port: Int, password: String): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password))

    final def apply(host: String, port: Int, database: Int): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, null, database))

    final def apply(host: String, port: Int, password: String, database: Int): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password, database))
  }

  final def apply[AIO[_]: effect.Async](config: RedisConfiuration, ttl: Option[Duration]): CacheProvider[AIO] =
    new CacheProvider[AIO](ttl) {
      import scalacache.serialization.circe._
      override implicit final val innerCache: Cache[Json] = RedisCache[Json](config.jedisPool)
    }

}
