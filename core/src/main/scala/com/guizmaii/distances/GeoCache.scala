package com.guizmaii.distances

import cats.effect.Async
import scalacache.CatsEffect.modes.async
import scalacache.{Cache, cachingF, Async => ScalaCacheAsync}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

abstract class GeoCache[S <: Serializable: ClassTag] {

  /**
    * Required by `cachingF`
    */
  implicit def scalacacheAsync[F[_]: Async]: ScalaCacheAsync[F] = async.M

  protected val expiration: Duration

  implicit val innerCache: Cache[S]

  final def getOrDefault[E[_]: Async](keyParts: Any*)(default: => E[S]): E[S] = cachingF(keyParts)(ttl = Some(expiration))(default)

}
