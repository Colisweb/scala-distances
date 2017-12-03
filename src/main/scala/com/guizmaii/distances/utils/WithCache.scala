package com.guizmaii.distances.utils

import com.guizmaii.distances.implementations.cache.{GeoCache, InMemoryGeoCache}

import scala.concurrent.duration._
import scala.reflect.ClassTag

private[distances] abstract class WithCache[E <: Serializable: ClassTag] {

  protected val alternativeCache: Option[GeoCache[E]]

  protected final val cache: GeoCache[E] = alternativeCache.getOrElse(InMemoryGeoCache[E](1 days))

}
