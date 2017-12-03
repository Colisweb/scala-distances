package com.guizmaii.distances.implementations.google.geocoder

import com.google.maps.GeocodingApi
import com.google.maps.model.ComponentFilter
import com.guizmaii.distances.Geocoder
import com.guizmaii.distances.Types.{LatLong, PostalCode}
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
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
final class GoogleGeocoder(
    geoApiContext: GoogleGeoApiContext,
    override protected val alternativeCache: Option[GeoCache[LatLong]] = None
) extends Geocoder {

  import com.guizmaii.distances.utils.MonixSchedulers.AlwaysAsyncForkJoinScheduler._
  import com.guizmaii.distances.utils.RichImplicits._

  override def geocodeT(postalCode: PostalCode): Task[LatLong] = {
    val fetch: Task[LatLong] =
      GeocodingApi
        .newRequest(geoApiContext.geoApiContext)
        .region("eu")
        .language("fr")
        .components(ComponentFilter.postalCode(postalCode.value))
        .toTask
        .map(_.head.geometry.location.toInnerLatLong)

    cache.getOrTask(postalCode)(fetch)
  }

  override def geocode(postalCode: PostalCode): CancelableFuture[LatLong] = geocodeT(postalCode).runAsync

}
