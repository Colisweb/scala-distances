package com.colisweb.distances

import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.colisweb.distances.Cache.{Caching, GetCached}
import com.colisweb.distances.DistanceProvider.{BatchDistanceF, DistanceF}
import com.colisweb.distances.Types._
import io.circe.{Decoder, Encoder}

import scala.collection.breakOut

class DistanceApi[F[_]: Async: Par, E](
    distanceF: DistanceF[F, E],
    batchDistanceF: BatchDistanceF[F, E],
    caching: Caching[F, Distance],
    getCached: GetCached[F, Distance]
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

  final def batchDistances(
      origins: List[LatLong],
      destinations: List[LatLong],
      travelMode: TravelMode,
      maybeTrafficHandling: Option[TrafficHandling] = None
  ): F[Map[(LatLong, LatLong), Either[E, Distance]]] = {
    val maybeCachedValues =
      origins
        .flatMap(origin => destinations.map(origin -> _))
        .parTraverse {
          case (origin, destination) =>
            val maybeCachedValue = getCached(decoder, travelMode, origin, destination, maybeTrafficHandling)
            ((origin, destination).pure[F] -> maybeCachedValue).bisequence
        }

    maybeCachedValues.flatMap { cachedList =>
      val (uncachedValues, cachedValues) = cachedList.partition { case (_, maybeCached) => maybeCached.isEmpty }

      val cachedMap        = handleCachedValues(cachedValues)
      val distanceBatchesF = handleUncachedValues(uncachedValues, travelMode, maybeTrafficHandling)

      distanceBatchesF.map { distanceBatches =>
        distanceBatches.foldLeft(cachedMap) {
          case (accMap, batches) =>
            accMap ++ batches
        }
      }
    }
  }

  private def handleCachedValues(
      cachedValues: List[((LatLong, LatLong), Option[Distance])]
  ): Map[(LatLong, LatLong), Either[E, Distance]] =
    cachedValues
      .map {
        case ((origin, destination), cachedDistance) =>
          (origin, destination) -> Right[E, Distance](cachedDistance.get)
      }
      .toMap[(LatLong, LatLong), Either[E, Distance]]

  private def handleUncachedValues(
      uncachedValues: List[((LatLong, LatLong), Option[Distance])],
      travelMode: TravelMode,
      maybeTrafficHandling: Option[TrafficHandling]
  ): F[List[Map[(LatLong, LatLong), Either[E, Distance]]]] = {

    def cacheIfSuccessful(
        origin: LatLong,
        destination: LatLong,
        errorOrDistance: Either[E, Distance]
    ): F[((LatLong, LatLong), Either[E, Distance])] = {
      val eitherF: F[Either[E, Distance]] = errorOrDistance match {
        case Right(distance) =>
          caching(distance, decoder, encoder, travelMode, origin, destination, maybeTrafficHandling)
            .map(Right[E, Distance])

        case Left(error) => error.pure[F].map(Left[E, Distance])
      }

      ((origin, destination).pure[F] -> eitherF).bisequence
    }

    val pairs         = uncachedValues.map { case (pair, _) => pair }
    val pairsByOrigin = pairs.groupBy { case (origin, _)    => origin }

    val groups =
      pairsByOrigin
        .groupBy {
          case (_, pairs) =>
            pairs.map(_._2).sortBy(destCoords => (destCoords.latitude, destCoords.longitude))
        }
        .mapValues(_.keys.toList)
        .toList

    groups.parTraverse {
      case (destinations, origins) =>
        batchDistanceF(travelMode, origins, destinations, maybeTrafficHandling)
          .flatMap { distancesMap =>
            distancesMap.toList
              .parTraverse {
                case ((origin, destination), errorOrDistance) =>
                  cacheIfSuccessful(origin, destination, errorOrDistance)
              }
              .map(_.toMap)
          }
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
          val eitherF: F[Either[E, Distance]] = errorOrDistance match {
            case Right(distance) =>
              caching(distance, decoder, encoder, mode, origin, destination, maybeTrafficHandling)
                .map(Right[E, Distance])

            case Left(error) => error.pure[F].map(Left[E, Distance])
          }

          (DirectedPath(origin, destination, mode, maybeTrafficHandling).pure[F] -> eitherF).bisequence
        }
      }
  }

}

object DistanceApi {
  val decoder: Decoder[Distance] = Distance.decoder
  val encoder: Encoder[Distance] = Distance.encoder

  final def apply[F[_]: Async: Par, E](
      distanceF: DistanceF[F, E],
      batchDistanceF: BatchDistanceF[F, E],
      caching: Caching[F, Distance],
      getCached: GetCached[F, Distance]
  ): DistanceApi[F, E] =
    new DistanceApi(distanceF, batchDistanceF, caching, getCached)

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
