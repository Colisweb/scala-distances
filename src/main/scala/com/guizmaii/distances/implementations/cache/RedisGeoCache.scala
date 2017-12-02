package com.guizmaii.distances.implementations.cache

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

final class RedisGeoCache[E <: Serializable: ClassTag](host: String, port: Int, override val expiration: Duration) extends GeoCache[E] {
  import scalacache._
  import scalacache.redis._
  import scalacache.serialization.binary._

  override implicit val innerCache: Cache[E] = RedisCache[E](host, port)
}

object RedisGeoCache {
  def apply[E <: Serializable: ClassTag](host: String, port: Int, expiration: Duration): RedisGeoCache[E] =
    new RedisGeoCache(host, port, expiration)
}
