package com.guizmaii.distances.providers

import cats.data.OptionT
import cats.effect
import cats.effect.Async
import io.circe.{Decoder, Encoder, Json}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool
import scalacache.Cache

import scala.concurrent.duration.Duration

abstract class CacheProvider[AIO[_]: Async] {

  import cats.implicits._
  import scalacache.CatsEffect.modes.async

  protected implicit val innerCache: Cache[Json]

  def get[A](key: Any*)(implicit decoder: Decoder[A]): AIO[Option[A]] =
    OptionT(innerCache.get(key)).mapFilter(decoder.decodeJson(_).toOption).value

  def set[A](key: Any*)(value: A, ttl: Option[Duration] = None)(implicit encoder: Encoder[A]): AIO[Json] = {
    val jsonValue = encoder.apply(value)
    innerCache.put(key)(jsonValue, ttl).map(_ => jsonValue)
  }

}

object InMemoryCacheProvider {

  import scalacache._
  import scalacache.caffeine._

  def apply[AIO[_]: effect.Async](): CacheProvider[AIO] = new CacheProvider[AIO]() {
    override protected implicit final val innerCache: Cache[Json] = CaffeineCache[Json]
  }

}

object RedisCacheProvider {

  import scalacache._
  import scalacache.redis._

  final case class RedisConfiuration(jedisPool: JedisPool) extends AnyVal

  object RedisConfiuration {
    def apply(host: String, port: Int): RedisConfiuration = RedisConfiuration(new JedisPool(host, port))

    def apply(host: String, port: Int, password: String): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password))

    def apply(host: String, port: Int, database: Int): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, null, database))

    def apply(host: String, port: Int, password: String, database: Int): RedisConfiuration =
      RedisConfiuration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password, database))
  }

  def apply[AIO[_]: effect.Async](config: RedisConfiuration): CacheProvider[AIO] = new CacheProvider[AIO] {
    import scalacache.serialization.circe._
    override protected implicit final val innerCache: Cache[Json] = RedisCache[Json](config.jedisPool)
  }

}
