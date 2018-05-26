package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, NonAmbiguousAddress, PostalCode}

class Geocoder[AIO[_]: Async](provider: GeoProvider[AIO], cache: Cache[AIO]) {

  final def geocodePostalCode(postalCode: PostalCode): AIO[LatLong] =
    cache.cachingF(postalCode) { provider.geocode(postalCode) }

  final def geocodeNonAmbiguousAddress(address: NonAmbiguousAddress): AIO[LatLong] =
    cache.cachingF(address) { provider.geocode(address) }

}

object Geocoder {
  final def apply[AIO[_]: Async](provider: GeoProvider[AIO], cacheProvider: Cache[AIO]): Geocoder[AIO] =
    new Geocoder(provider, cacheProvider)
}
