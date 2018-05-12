package com.guizmaii.distances.utils

import cats.effect.Async
import com.guizmaii.distances.Types.LatLong
import com.guizmaii.distances.providers.{CacheProvider, DistanceProvider, GeoProvider}
import com.guizmaii.distances.{Geocoder, Types}
import io.circe.Json
import scalacache.{Cache, CacheConfig, Flags, Mode}

import scala.concurrent.duration.Duration

object Stubs {

  class NonCachingCache extends Cache[Json] {
    override def config: CacheConfig = ???

    override def cachingForMemoize[F[_]](baseKey: String)(ttl: Option[Duration])(f: => Json)(implicit mode: Mode[F],
                                                                                             flags: Flags): F[Json] = ???

    override def cachingForMemoizeF[F[_]](baseKey: String)(ttl: Option[Duration])(f: => F[Json])(implicit mode: Mode[F],
                                                                                                 flags: Flags): F[Json] = ???

    override def get[F[_]](keyParts: Any*)(implicit mode: Mode[F], flags: Flags): F[Option[Json]] = ???

    override def put[F[_]](keyParts: Any*)(value: Json, ttl: Option[Duration])(implicit mode: Mode[F], flags: Flags): F[Any] = ???

    override def remove[F[_]](keyParts: Any*)(implicit mode: Mode[F]): F[Any] = ???

    override def removeAll[F[_]]()(implicit mode: Mode[F]): F[Any] = ???

    override def caching[F[_]](keyParts: Any*)(ttl: Option[Duration])(f: => Json)(implicit mode: Mode[F], flags: Flags): F[Json] = ???

    override def cachingF[F[_]](keyParts: Any*)(ttl: Option[Duration])(f: => F[Json])(implicit mode: Mode[F], flags: Flags): F[Json] = f

    override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = ???
  }

  def cacheProviderStub[AIO[_]: Async]: CacheProvider[AIO] = new CacheProvider[AIO](None) {
    override private[distances] implicit val innerCache: Cache[Json] = new NonCachingCache
  }

  def distanceProviderStub[AIO[_]: Async]: DistanceProvider[AIO] = new DistanceProvider[AIO] {
    override def distance(mode: Types.TravelMode, origin: LatLong, destination: LatLong): AIO[Types.Distance] = ???
  }

  def geoProviderStub[AIO[_]: Async]: GeoProvider[AIO] = new GeoProvider[AIO] {
    override def geocode(point: Types.Point): AIO[LatLong] = ???
  }

  def geocoderStub[AIO[_]: Async] = new Geocoder[AIO](geoProviderStub, cacheProviderStub)

}
