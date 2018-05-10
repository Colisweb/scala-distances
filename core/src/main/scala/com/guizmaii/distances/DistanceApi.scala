package com.guizmaii.distances

import cats.effect.Async
import cats.temp.par.Par
import com.guizmaii.distances.Types._

final class DistanceApi[AIO[_]: Par](provider: DistanceProvider[AIO])(implicit AIO: Async[AIO]) {

  import cats.implicits._
  import cats.temp.par._

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      distances(DirectedPath(origin = origin, destination = destination, travelModes = travelModes) :: Nil)
        .map(_.map { case ((travelMode, _, _), distance) => travelMode -> distance })

  def distanceFromPostalCodes(geocoder: Geocoder[AIO])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      (geocoder.geocodePostalCode(origin), geocoder.geocodePostalCode(origin)).parMapN { case (o, d) => distance(o, d, travelModes) }.flatten

  def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]] = provider.distances(paths)

}

object DistanceApi {
  def apply[AIO[_]: Async: Par](provider: DistanceProvider[AIO]): DistanceApi[AIO] = new DistanceApi(provider)
}
