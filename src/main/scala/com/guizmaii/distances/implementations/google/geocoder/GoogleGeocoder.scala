package com.guizmaii.distances.implementations.google.geocoder

import com.google.maps.model.ComponentFilter
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.guizmaii.distances.Types.{LatLong, PostalCode}
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{Geocoder, Types}
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

  override def geocodeNonAmbigueAddressT(address: Types.Address)(implicit cache: GeoCache[LatLong]): Task[LatLong] = {
    val fetch: Task[LatLong] =
      rawRequest
        .components(ComponentFilter.country(address.country))
        .components(ComponentFilter.postalCode(address.postalCode.value))
        .address(s"${address.line1} ${address.line2} ${address.town}")
        .toTask
        .map(_.head.geometry.location.toInnerLatLong)

    cache.getOrTask(address)(fetch)
  }

  override def geocodeNonAmbigueAddress(address: Types.Address)(implicit cache: GeoCache[LatLong]): CancelableFuture[LatLong] =
    geocodeNonAmbigueAddressT(address).runAsync
}

object GoogleGeocoder {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleGeocoder = new GoogleGeocoder(geoApiContext)

}
