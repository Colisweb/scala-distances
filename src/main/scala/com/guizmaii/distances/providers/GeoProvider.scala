package com.guizmaii.distances.providers

import cats.effect.Async
import com.google.maps.model.{ComponentFilter, LatLng => GoogleLatLng}
import com.google.maps.{GeocodingApi, GeocodingApiRequest}
import com.guizmaii.distances.Types.{LatLong, NonAmbigueAddress, PostalCode, _}
import com.guizmaii.distances.providers.GoogleDistanceProvider.GoogleGeoApiContext

abstract class GeoProvider[AIO[_]: Async] {

  private[distances] def geocode(point: Point): AIO[LatLong]

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

  final def apply[AIO[_]: Par](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): GeoProvider[AIO] = new GeoProvider[AIO] {

    private final def rawRequest: GeocodingApiRequest =
      GeocodingApi
        .newRequest(geoApiContext.geoApiContext)
        .region("eu")
        .language("fr")

    override private[distances] final def geocode(point: Point): AIO[LatLong] = point match {

      case postalCode: PostalCode =>
        rawRequest
          .components(ComponentFilter.postalCode(postalCode.value))
          .asEffect
          .map(r => asLatLong(r.head.geometry.location))

      /*
       * Doc about "non ambigue addresses": https://developers.google.com/maps/documentation/geocoding/best-practices#complete-address
       *
       *
       * WARNING:
       * --------
       *
       * The actual implementation tries to be robust: it will try to always give you an answer:
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
       * TODO: The next step for this lib is to implement this more clever solution, but for now, use this method preferably with good quality data and/or with caution.
       */
      case address: NonAmbigueAddress =>
        def fetch(addr: NonAmbigueAddress): AIO[LatLong] =
          rawRequest
            .components(ComponentFilter.country(addr.country))
            .components(ComponentFilter.postalCode(addr.postalCode))
            .address(s"${addr.line1} ${addr.line2} ${addr.town}")
            .asEffect
            .map(r => asLatLong(r.head.geometry.location))

        fetch(address)
          .handleErrorWith {
            case _: NoSuchElementException =>
              (
                fetch(address.copy(line2 = "")),
                fetch(address.copy(line2 = "", town = "")),
                geocode(PostalCode(address.postalCode))
              ).raceInOrder3
          }
    }
  }

  @inline
  private[this] final def asLatLong(googleLatLng: GoogleLatLng): LatLong =
    LatLong(latitude = googleLatLng.lat, longitude = googleLatLng.lng)

}
