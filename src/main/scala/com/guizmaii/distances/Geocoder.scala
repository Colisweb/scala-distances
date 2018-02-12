package com.guizmaii.distances

import com.guizmaii.distances.Types.{Address, LatLong, PostalCode}
import com.guizmaii.distances.utils.WithCache
import monix.eval.Task
import monix.execution.CancelableFuture

trait Geocoder extends WithCache[LatLong] {
  def geocodePostalCodeT(postalCode: PostalCode): Task[LatLong]
  def geocodePostalCode(postalCode: PostalCode): CancelableFuture[LatLong]

  /**
    * Doc about "non ambigue addresses": https://developers.google.com/maps/documentation/geocoding/best-practices#complete-address
    *
    * @param address
    * @return
    */
  def geocodeNonAmbigueAddressT(address: Address): Task[LatLong]
  def geocodeNonAmbigueAddress(address: Address): CancelableFuture[LatLong]
}
