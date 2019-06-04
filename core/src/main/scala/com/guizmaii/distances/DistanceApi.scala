package com.guizmaii.distances

import java.time.Instant

import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.guizmaii.distances.Cache.CachingF
import com.guizmaii.distances.DistanceProvider.DistanceF
import com.guizmaii.distances.Types._

import scala.collection.breakOut

class DistanceApi[F[_]: Async: Par](distanceF: DistanceF[F], cachingF: CachingF[F, Distance]) {

  import DistanceApi._
  import cats.implicits._
  import cats.temp.par._
  import com.guizmaii.distances.utils.Implicits._

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
          distances.map { case ((travelMode, _, _), distance) => travelMode -> distance }.toMap
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
  ): F[Map[(TravelMode, LatLong, LatLong), Distance]] = {
    val combinedDirectedPaths: List[DirectedPath] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup, breakOut)

    combinedDirectedPaths
      .parFlatTraverse {
        case DirectedPath(origin, destination, travelModes) =>
          if (origin == destination)
            travelModes.map(mode => (mode, origin, destination) -> Distance.zero).pure[F]
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
  ): F[List[((TravelMode, LatLong, LatLong), Distance)]] = {
    modes
      .parTraverse { mode =>
        val distanceFn = distanceF(mode, origin, destination, maybeDepartureTime)

        cachingF(distanceFn, Distance.decoder, Distance.encoder, mode, origin, destination, maybeDepartureTime)
          .map((mode, origin, destination) -> _)
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
