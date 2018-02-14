package com.guizmaii.distances.implementations.google.geocoder

import com.google.maps.model.ComponentFilter
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.guizmaii.distances.Types.{Address, LatLong, PostalCode}
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{GeoCache, Geocoder}
import monix.eval.Task
import monix.execution.CancelableFuture

/**
  * Remarques:
  * ---------
  *
  * Les résultats de Geocoding de code postaux sont très hasardeux si l'on ne précise que le code postal.
  * Par exemple, si l'on utilise "59000", la réponse sera la bonne tandis que si l'on utilise "59130" la réponse
  * sera vide donc fausse.
  *
  * Il est possible de préciser d'autres "components" comme le "country" cependant nous n'avons pas forcément le pays.
  *
  * Un astuce qui a été trouvé est de préciser la "region" comme étant "eu" (Europe). Cela semble améliorer les
  * résultat. Par exemple, avec "region=eu", "59130" obtient une réponse valide.
  *
  * Seconde astuce, il faut également préciser le "language" sinon un pourcentage élevé de réponses sont fausses.
  */
final class GoogleGeocoder(geoApiContext: GoogleGeoApiContext) extends Geocoder {

  import com.guizmaii.distances.utils.RichImplicits._
  import monix.execution.Scheduler.Implicits.global

  private def rawRequest: GeocodingApiRequest =
    GeocodingApi
      .newRequest(geoApiContext.geoApiContext)
      .region("eu")
      .language("fr")

  override def geocodePostalCodeT(postalCode: PostalCode)(implicit cache: GeoCache[LatLong]): Task[LatLong] = {
    val fetch: Task[LatLong] =
      rawRequest
        .components(ComponentFilter.postalCode(postalCode.value))
        .toTask
        .map(_.head.geometry.location.toInnerLatLong)

    cache.getOrTask(postalCode)(fetch)
  }

  override def geocodePostalCode(postalCode: PostalCode)(implicit cache: GeoCache[LatLong]): CancelableFuture[LatLong] =
    geocodePostalCodeT(postalCode).runAsync

  override def geocodeNonAmbigueAddressT(address: Address)(implicit cache: GeoCache[LatLong]): Task[LatLong] = {
    def fetch(addr: Address): Task[LatLong] =
      rawRequest
        .components(ComponentFilter.country(addr.country))
        .components(ComponentFilter.postalCode(addr.postalCode.value))
        .address(s"${addr.line1} ${addr.line2} ${addr.town}")
        .toTask
        .map(_.head.geometry.location.toInnerLatLong)

    def fallbackFetch(addr: Address): Task[LatLong] =
      fetch(addr)
        .onErrorRecoverWith {
          case _: NoSuchElementException =>
            Task.raceInOrder3(fetch(addr.copy(line2 = "")), fetch(addr.copy(line2 = "", town = "")), geocodePostalCodeT(addr.postalCode))
        }

    cache.getOrTask(address)(fallbackFetch(address))
  }

  override def geocodeNonAmbigueAddress(address: Address)(implicit cache: GeoCache[LatLong]): CancelableFuture[LatLong] =
    geocodeNonAmbigueAddressT(address).runAsync
}

object GoogleGeocoder {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleGeocoder = new GoogleGeocoder(geoApiContext)
}
