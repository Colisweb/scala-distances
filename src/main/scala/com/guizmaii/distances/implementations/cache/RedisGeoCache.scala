package com.guizmaii.distances.implementations.cache

import com.guizmaii.distances.GeoCache
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

final class RedisGeoCache[E <: Serializable: ClassTag](jedisPool: JedisPool, override val expiration: Duration) extends GeoCache[E] {
  import scalacache._
  import scalacache.redis._
  import scalacache.serialization.binary._

  override implicit val innerCache: Cache[E] = RedisCache[E](jedisPool)
}

object RedisGeoCache {
  def apply[E <: Serializable: ClassTag](host: String, port: Int, expiration: Duration): RedisGeoCache[E] =
    new RedisGeoCache(new JedisPool(host, port), expiration)

  def apply[E <: Serializable: ClassTag](host: String, port: Int, password: String, expiration: Duration): RedisGeoCache[E] =
    new RedisGeoCache(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password), expiration)

  def apply[E <: Serializable: ClassTag](host: String, port: Int, expiration: Duration, database: Int): RedisGeoCache[E] =
    new RedisGeoCache(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, null, database), expiration)

  def apply[E <: Serializable: ClassTag](host: String, port: Int, password: String, expiration: Duration, database: Int): RedisGeoCache[E] =
    new RedisGeoCache(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password, database), expiration)
}
