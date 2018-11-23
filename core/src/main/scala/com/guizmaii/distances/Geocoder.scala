package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, NonAmbiguousAddress, PostalCode}

class Geocoder[F[_]: Async](provider: GeoProvider[F], cache: Cache[F]) {

  final def geocodePostalCode(postalCode: PostalCode): F[LatLong] =
    cache.cachingF(postalCode) { provider.geocode(postalCode) }

  final def geocodeNonAmbiguousAddress(address: NonAmbiguousAddress): F[LatLong] =
    cache.cachingF(address) { provider.geocode(address) }

}

object Geocoder {
  final def apply[F[_]: Async](provider: GeoProvider[F], cacheProvider: Cache[F]): Geocoder[F] =
    new Geocoder(provider, cacheProvider)
}
