package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, NonAmbigueAddress, PostalCode}
import com.guizmaii.distances.providers.{CacheProvider, GeoProvider, InMemoryCacheProvider}

import scala.concurrent.duration._
import scala.language.postfixOps

class Geocoder[AIO[_]: Async](provider: GeoProvider[AIO], cacheProvider: CacheProvider[AIO]) {

  final def geocodePostalCode(postalCode: PostalCode): AIO[LatLong] =
    cacheProvider.cachingF(postalCode) { provider.geocode(postalCode) }

  final def geocodeNonAmbigueAddress(address: NonAmbigueAddress): AIO[LatLong] =
    cacheProvider.cachingF(address) { provider.geocode(address) }

}

object Geocoder {
  final def apply[AIO[_]: Async](provider: GeoProvider[AIO]): Geocoder[AIO] = new Geocoder(provider, InMemoryCacheProvider(Some(7 days)))
}
