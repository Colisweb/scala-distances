package com.colisweb.distances

import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.colisweb.distances.Cache.CachingF
import com.colisweb.distances.DistanceProvider.{BatchDistanceF, DistanceF}
import com.colisweb.distances.Types._

import scala.collection.breakOut

class DistanceApi[F[_]: Async: Par, E](
    distanceF: DistanceF[F, E],
    batchDistanceF: BatchDistanceF[F, E],
    cachingF: CachingF[F, Distance]
) {

  import DistanceApi._
  import cats.implicits._
  import cats.temp.par._
  import com.colisweb.distances.utils.Implicits._

  final def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[TravelMode, Either[E, Distance]]] =
    if (origin == destination)
      Async[F].pure(travelModes.map(_ -> Right(Distance.zero))(breakOut))
    else
      parDistances(travelModes, origin, destination, maybeTrafficHandling)
        .map { distancesOrErrors =>
          distancesOrErrors.map { case (path, distanceOrError) => path.travelMode -> distanceOrError }.toMap
        }

  // TODO: Check if there are already cached distances, and avoid these calls
  final def batchDistances(
      origins: List[LatLong],
      destinations: List[LatLong],
      travelMode: TravelMode,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[(LatLong, LatLong), Either[E, Distance]]] = {
    batchDistanceF(travelMode, origins, destinations, maybeTrafficHandling)
      .flatMap { distancesMap =>
        distancesMap.toList
          .parTraverse {
            case ((origin, destination), errorOrDistance) =>
              val eitherF: F[Either[E, Distance]] = errorOrDistance match {
                case Right(distance) =>
                  cachingF(
                    distance.pure[F],
                    Distance.decoder,
                    Distance.encoder,
                    travelMode,
                    origin,
                    destination,
                    maybeTrafficHandling
                  ).map(Right[E, Distance])

                case Left(error) => error.pure[F].map(Left[E, Distance])
              }

              ((origin, destination).pure[F] -> eitherF).bisequence
          }
          .map(_.toMap)
      }
  }

  final def distanceFromPostalCodes(geocoder: Geocoder[F])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode],
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[TravelMode, Either[E, Distance]]] =
    if (origin == destination) Async[F].pure(travelModes.map(_ -> Right(Distance.zero))(breakOut))
    else
      (
        geocoder.geocodePostalCode(origin),
        geocoder.geocodePostalCode(destination)
      ).parTupled.flatMap { case (orig, dest) => distance(orig, dest, travelModes, maybeTrafficHandling) }

  final def distances(paths: Seq[DirectedPathMultipleModes]): F[Map[DirectedPath, Either[E, Distance]]] = {
    val combinedDirectedPaths: List[DirectedPathMultipleModes] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPathMultipleModes(origin, destination, _, _) => (origin, destination) }(
          directedPathSemiGroup,
          breakOut
        )

    combinedDirectedPaths
      .parFlatTraverse {
        case DirectedPathMultipleModes(origin, destination, travelModes, maybeDepartureTime) =>
          if (origin == destination)
            travelModes
              .map { mode =>
                val zero: Either[E, Distance] = Right[E, Distance](Distance.zero)
                DirectedPath(origin, destination, mode, maybeDepartureTime) -> zero
              }
              .pure[F]
          else
            parDistances(travelModes, origin, destination, maybeDepartureTime)
      }
      .map(_.toMap)
  }

  private[this] final def parDistances(
      modes: List[TravelMode],
      origin: LatLong,
      destination: LatLong,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[List[(DirectedPath, Either[E, Distance])]] = {
    modes
      .parTraverse { mode =>
        distanceF(mode, origin, destination, maybeTrafficHandling).flatMap { errorOrDistance =>
          val directedPath = DirectedPath(origin, destination, mode, maybeTrafficHandling)

          val eitherF: F[Either[E, Distance]] = errorOrDistance match {
            case Right(distance) =>
              cachingF(
                distance.pure[F],
                Distance.decoder,
                Distance.encoder,
                mode,
                origin,
                destination,
                maybeTrafficHandling
              ).map(Right[E, Distance])

            case Left(error) => error.pure[F].map(Left[E, Distance])
          }

          (directedPath.pure[F] -> eitherF).bisequence
        }
      }
  }

}

object DistanceApi {
  final def apply[F[_]: Async: Par, E](
      distanceF: DistanceF[F, E],
      batchDistanceF: BatchDistanceF[F, E],
      cachingF: CachingF[F, Distance]
  ): DistanceApi[F, E] =
    new DistanceApi(distanceF, batchDistanceF, cachingF)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPathMultipleModes] =
    new Semigroup[DirectedPathMultipleModes] {
      override def combine(x: DirectedPathMultipleModes, y: DirectedPathMultipleModes): DirectedPathMultipleModes =
        DirectedPathMultipleModes(
          origin = x.origin,
          destination = x.destination,
          (x.travelModes ++ y.travelModes).distinct
        )
    }
}
