package com.colisweb.distances

import java.time.Instant

import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.colisweb.distances.Cache.CachingF
import com.colisweb.distances.DistanceProvider.DistanceF
import com.colisweb.distances.Types._

import scala.collection.breakOut

class DistanceApi[F[_]: Async: Par](distanceF: DistanceF[F], cachingF: CachingF[F, Distance]) {

  import DistanceApi._
  import cats.implicits._
  import cats.temp.par._
  import com.colisweb.distances.utils.Implicits._

  final def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode],
      maybeDepartureTime: Option[Instant] = None
  ): F[Map[TravelMode, Distance]] =
    if (origin == destination)
      Async[F].pure(travelModes.map(_ -> Distance.zero)(breakOut))
    else
      parDistances(travelModes, origin, destination, maybeDepartureTime)
        .map { distances =>
          distances.map { case ((travelMode, _, _, _), distance) => travelMode -> distance }.toMap
        }

  final def distanceFromPostalCodes(geocoder: Geocoder[F])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode],
      maybeDepartureTime: Option[Instant] = None
  ): F[Map[TravelMode, Distance]] =
    if (origin == destination) Async[F].pure(travelModes.map(_ -> Distance.zero)(breakOut))
    else
      (
        geocoder.geocodePostalCode(origin),
        geocoder.geocodePostalCode(destination)
      ).parTupled.flatMap { case (orig, dest) => distance(orig, dest, travelModes, maybeDepartureTime) }

  final def distances(
      paths: Seq[DirectedPath],
      maybeDepartureTime: Option[Instant] = None
  ): F[Map[(TravelMode, LatLong, LatLong, Option[Instant]), Distance]] = {
    val combinedDirectedPaths: List[DirectedPath] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup, breakOut)

    combinedDirectedPaths
      .parFlatTraverse {
        case DirectedPath(origin, destination, travelModes) =>
          if (origin == destination)
            travelModes.map(mode => (mode, origin, destination, maybeDepartureTime) -> Distance.zero).pure[F]
          else
            parDistances(travelModes, origin, destination, maybeDepartureTime)
      }
      .map(_.toMap)
  }

  private[this] final def parDistances(
      modes: List[TravelMode],
      origin: LatLong,
      destination: LatLong,
      maybeDepartureTime: Option[Instant]
  ): F[List[((TravelMode, LatLong, LatLong, Option[Instant]), Distance)]] = {
    modes
      .parTraverse { mode =>
        val distance = distanceF(mode, origin, destination, maybeDepartureTime)

        cachingF(distance, Distance.decoder, Distance.encoder, mode, origin, destination, maybeDepartureTime)
          .map((mode, origin, destination, maybeDepartureTime) -> _)
      }
  }

}

object DistanceApi {
  final def apply[F[_]: Async: Par](distanceF: DistanceF[F], cachingF: CachingF[F, Distance]): DistanceApi[F] =
    new DistanceApi(distanceF, cachingF)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPath] =
    new Semigroup[DirectedPath] {
      override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
        DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
    }
}
