package com.guizmaii.distances

import cats.effect.Effect
import com.guizmaii.distances.Types.{Address, LatLong, PostalCode}

trait Geocoder {

  def geocodePostalCode[E[_]: Effect](postalCode: PostalCode)(implicit cache: GeoCache[LatLong]): E[LatLong]

  /**
    * Doc about "non ambigue addresses": https://developers.google.com/maps/documentation/geocoding/best-practices#complete-address
    *
    *
    * WARNING:
    * --------
    *
    * The actual implementation (GoogleGeocoder) tries to be robust: it will try to always give you an answer:
    *
    *   1. If it doesn't find an answer with the address informations, it will try to geocode the address without the `line2` information.
    *   2. If it still doesn't find an anwser, it will try to geocode the address without the `line2` and wihout the `town` information.
    *   3. Last, if it still doesn't find an answer, it will geocode the postal code.
    *
    * This situation is not ideal because, in the situtation 2., the geocoder can potentially gives you a wrong answer.
    *
    * As the name of this method indicates, this geocoder method should normally be stric: it finds the exact location or it doesn't find any.
    *
    * For a more clever geocoder, Google proposes a better solution that what we're doing here.
    * See: https://developers.google.com/maps/documentation/geocoding/best-practices#automated-system
    *
    * The next step for this lib is to implement this more clever solution, but for now, use this method preferably with good quality data and/or with caution.
    *
    * @param address
    * @return
    */
  def geocodeNonAmbigueAddress[E[_]: Effect](address: Address)(implicit cache: GeoCache[LatLong]): E[LatLong]

}
