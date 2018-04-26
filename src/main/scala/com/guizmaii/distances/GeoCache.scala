package com.guizmaii.distances

import monix.eval.Task

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

abstract class GeoCache[E <: Serializable: ClassTag] {

  import scalacache.Monix.modes._
  import scalacache._

  protected val expiration: Duration

  implicit val innerCache: Cache[E]

  final def getOrTask(keyParts: Any*)(orElse: => Task[E]): Task[E] = cachingF(keyParts)(ttl = Some(expiration))(orElse)

}
