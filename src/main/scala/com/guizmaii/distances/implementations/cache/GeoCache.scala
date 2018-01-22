package com.guizmaii.distances.implementations.cache

import monix.eval.Task

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

abstract class GeoCache[E <: Serializable: ClassTag] {

  import scalacache._

  // TODO: Wait an answer for: https://twitter.com/guizmaii/status/934923692148232193
  implicit val task: Mode[Task] = new Mode[Task] {
    val M: Async[Task] = CatsEffect.asyncForCatsEffectAsync[Task]
  }

  protected val expiration: Duration

  implicit val innerCache: Cache[E]

  final def getOrTask(keyParts: Any*)(orElse: => Task[E]): Task[E] = cachingF(keyParts)(ttl = Some(expiration))(orElse)

  final def setT(keyParts: Any*)(value: E): Task[Any] = put(keyParts)(value = value, ttl = Some(expiration))

}
