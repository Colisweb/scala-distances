package com.guizmaii.distances

import java.time.Instant

import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.guizmaii.distances.Types._

import scala.collection.breakOut

class DistanceApi[F[_]: Async: Par](distanceProvider: DistanceProvider[F], cache: Cache[F]) {

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
    if (origin == destination) Async[F].pure(travelModes.map(_ -> Distance.zero)(breakOut))
    else
      travelModes
        .parTraverse { mode =>
          cache
            .cachingF(mode, origin, destination, maybeDepartureTime) {
              maybeDepartureTime match {
                case Some(departureTime) => distanceProvider.distanceAtDepartureTime(mode, origin, destination, departureTime)
                case None                => distanceProvider.distance(mode, origin, destination)
              }
            }
            .map(mode -> _)
        }
        .map(_.toMap)

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
          if (origin == destination) travelModes.map(mode => (mode, origin, destination) -> Distance.zero).pure[F]
          else {
            travelModes
              .parTraverse { mode =>
                cache
                  .cachingF(mode, origin, destination, maybeDepartureTime) {
                    maybeDepartureTime match {
                      case Some(departureTime) => distanceProvider.distanceAtDepartureTime(mode, origin, destination, departureTime)
                      case None                => distanceProvider.distance(mode, origin, destination)
                    }
                  }
                  .map((mode, origin, destination) -> _)
              }
          }
      }
      .map(_.toMap)
  }
}

object DistanceApi {
  final def apply[F[_]: Async: Par](provider: DistanceProvider[F], cacheProvider: Cache[F]): DistanceApi[F] =
    new DistanceApi(provider, cacheProvider)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPath] =
    new Semigroup[DirectedPath] {
      override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
        DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
    }
}
