package com.guizmaii.distances

import com.guizmaii.distances.Types.{Address, LatLong, PostalCode}
import monix.eval.Task
import monix.execution.CancelableFuture

trait Geocoder {

  def geocodePostalCodeT(postalCode: PostalCode)(implicit cache: GeoCache[LatLong]): Task[LatLong]
  def geocodePostalCode(postalCode: PostalCode)(implicit cache: GeoCache[LatLong]): CancelableFuture[LatLong]

  /**
    * Doc about "non ambigue addresses": https://developers.google.com/maps/documentation/geocoding/best-practices#complete-address
    *
    * @param address
    * @return
    */
  def geocodeNonAmbigueAddressT(address: Address)(implicit cache: GeoCache[LatLong]): Task[LatLong]
  def geocodeNonAmbigueAddress(address: Address)(implicit cache: GeoCache[LatLong]): CancelableFuture[LatLong]
}
