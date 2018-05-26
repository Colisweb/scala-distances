package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, NonAmbiguousAddress, PostalCode}
import com.guizmaii.distances.providers.{CacheProvider, GeoProvider, InMemoryCacheProvider}

import scala.concurrent.duration._

class Geocoder[AIO[_]: Async](provider: GeoProvider[AIO], cacheProvider: CacheProvider[AIO]) {

  final def geocodePostalCode(postalCode: PostalCode): AIO[LatLong] =
    cacheProvider.cachingF(postalCode) { provider.geocode(postalCode) }

  final def geocodeNonAmbiguousAddress(address: NonAmbiguousAddress): AIO[LatLong] =
    cacheProvider.cachingF(address) { provider.geocode(address) }

}

object Geocoder {
  final def apply[AIO[_]: Async](provider: GeoProvider[AIO], ttl: Option[Duration]): Geocoder[AIO] =
    new Geocoder(provider, InMemoryCacheProvider(ttl))

  final def apply[AIO[_]: Async](provider: GeoProvider[AIO], cacheProvider: CacheProvider[AIO]): Geocoder[AIO] =
    new Geocoder(provider, cacheProvider)
}
