package com.colisweb.distances.caches

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

final case class RedisConfiguration(jedisPool: JedisPool) extends AnyVal

object RedisConfiguration {
  final def apply(host: String, port: Int): RedisConfiguration = RedisConfiguration(new JedisPool(host, port))

  final def apply(host: String, port: Int, password: String): RedisConfiguration =
    RedisConfiguration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password))

  final def apply(host: String, port: Int, database: Int): RedisConfiguration =
    RedisConfiguration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, null, database))

  final def apply(host: String, port: Int, password: String, database: Int): RedisConfiguration =
    RedisConfiguration(new JedisPool(new GenericObjectPoolConfig(), host, port, 1000, password, database))
}
