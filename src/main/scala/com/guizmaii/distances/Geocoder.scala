package com.guizmaii.distances

import cats.effect.Async
import com.guizmaii.distances.Types.{LatLong, NonAmbigueAddress, PostalCode}
import com.guizmaii.distances.providers.GeoProvider

final class Geocoder[AIO[_]: Async](provider: GeoProvider[AIO]) {

  @inline def geocodePostalCode(postalCode: PostalCode): AIO[LatLong] = provider.geocode(postalCode)

  @inline def geocodeNonAmbigueAddress(address: NonAmbigueAddress): AIO[LatLong] = provider.geocode(address)

}

object Geocoder {
  @inline def apply[AIO[_]: Async](provider: GeoProvider[AIO]): Geocoder[AIO] = new Geocoder(provider)
}
