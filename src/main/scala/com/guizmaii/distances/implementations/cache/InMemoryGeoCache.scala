package com.guizmaii.distances.implementations.cache

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

final class InMemoryGeoCache[E <: Serializable: ClassTag](override val expiration: Duration) extends GeoCache[E] {
  import scalacache._
  import scalacache.caffeine._

  override implicit val innerCache: Cache[E] = CaffeineCache[E]
}

object InMemoryGeoCache {
  def apply[E <: Serializable: ClassTag](expiration: Duration): InMemoryGeoCache[E] = new InMemoryGeoCache(expiration)
}
