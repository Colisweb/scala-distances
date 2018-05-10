package com.guizmaii.distances

import cats.effect.Async
import com.google.maps.model.ComponentFilter
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.guizmaii.distances.Types.{LatLong, NonAmbigueAddress, PostalCode, _}
import com.guizmaii.distances.utils.GoogleGeoApiContext

abstract class GeoProvider[AIO[_]: Async] {

  def geocode(point: Point): AIO[LatLong]

}

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
object GoogleGeoProvider {

  import cats.implicits._
  import cats.temp.par._
  import com.guizmaii.distances.utils.RichImplicits._

  def apply[AIO[_]: Par](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): GeoProvider[AIO] = new GeoProvider[AIO] {

    private def rawRequest: GeocodingApiRequest =
      GeocodingApi
        .newRequest(geoApiContext.geoApiContext)
        .region("eu")
        .language("fr")

    override def geocode(point: Point): AIO[LatLong] = point match {

      case postalCode: PostalCode =>
        rawRequest
          .components(ComponentFilter.postalCode(postalCode.value))
          .asEffect
          .map(_.head.geometry.location.toInnerLatLong)

      case address: NonAmbigueAddress =>
        def fetch(addr: NonAmbigueAddress): AIO[LatLong] =
          rawRequest
            .components(ComponentFilter.country(addr.country))
            .components(ComponentFilter.postalCode(addr.postalCode.value))
            .address(s"${addr.line1} ${addr.line2} ${addr.town}")
            .asEffect
            .map(_.head.geometry.location.toInnerLatLong)

        fetch(address)
          .handleErrorWith {
            case _: NoSuchElementException =>
              (
                fetch(address.copy(line2 = "")),
                fetch(address.copy(line2 = "", town = "")),
                geocode(address.postalCode)
              ).raceInOrder3
          }
    }
  }

}
