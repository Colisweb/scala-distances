package com.guizmaii.distances

import cats.Parallel
import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.guizmaii.distances.Types._
import com.guizmaii.distances.providers.{CacheProvider, DistanceProvider, InMemoryCacheProvider}

import scala.concurrent.duration._

class DistanceApi[AIO[_]: Par](distanceProvider: DistanceProvider[AIO], cacheProvider: CacheProvider[AIO])(implicit AIO: Async[AIO]) {

  import DistanceApi._
  import cats.implicits._
  import cats.temp.par._
  import com.guizmaii.distances.utils.RichImplicits._

  final def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      travelModes
        .parTraverse { mode =>
          cacheProvider
            .cachingF(mode, origin, destination) {
              distanceProvider.distance(mode, origin, destination)
            }
            .map(mode -> _)
        }
        .map(_.toMap)

  final def distanceFromPostalCodes(geocoder: Geocoder[AIO])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      (geocoder.geocodePostalCode(origin), geocoder.geocodePostalCode(destination)).parMapN { case (o, d) => distance(o, d, travelModes) }.flatten

  final def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]] = {
    val combinedDirectedPaths: List[DirectedPath] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup)

    // TODO: Replace by the syntax extension when this issue is closed: https://github.com/typelevel/cats/issues/2255
    Parallel
      .parFlatTraverse(combinedDirectedPaths) {
        case DirectedPath(origin, destination, travelModes) =>
          if (origin == destination) travelModes.traverse(mode => AIO.pure(Distance.zero).map((mode, origin, destination) -> _))
          else {
            travelModes.parTraverse { mode =>
              cacheProvider
                .cachingF(mode, origin, destination) {
                  distanceProvider.distance(mode, origin, destination)
                }
                .map((mode, origin, destination) -> _)
            }
          }
      }
      .map(_.toMap)
  }
}

object DistanceApi {
  final def apply[AIO[_]: Async: Par](provider: DistanceProvider[AIO], ttl: Option[Duration]): DistanceApi[AIO] =
    new DistanceApi(provider, InMemoryCacheProvider(ttl))

  final def apply[AIO[_]: Async: Par](provider: DistanceProvider[AIO], cacheProvider: CacheProvider[AIO]): DistanceApi[AIO] =
    new DistanceApi(provider, cacheProvider)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPath] = new Semigroup[DirectedPath] {
    override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
      DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
  }
}
