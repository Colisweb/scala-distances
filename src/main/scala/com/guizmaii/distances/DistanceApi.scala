package com.guizmaii.distances

import cats.Parallel
import cats.effect.Async
import cats.kernel.Semigroup
import cats.temp.par.Par
import com.guizmaii.distances.Types._

final class DistanceApi[AIO[_]: Par](provider: DistanceProvider[AIO])(implicit AIO: Async[AIO]) {

  import DistanceApi._
  import cats.implicits._
  import cats.temp.par._
  import com.guizmaii.distances.utils.RichImplicits._

  def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      travelModes
        .traverse(mode => provider.distance(mode, origin, destination).map(mode -> _))
        .map(_.toMap)

  def distanceFromPostalCodes(geocoder: Geocoder[AIO])(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode]
  ): AIO[Map[TravelMode, Distance]] =
    if (origin == destination) AIO.pure(travelModes.map(_ -> Distance.zero).toMap)
    else
      (geocoder.geocodePostalCode(origin), geocoder.geocodePostalCode(destination)).parMapN { case (o, d) => distance(o, d, travelModes) }.flatten

  def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]] = {
    @inline def format(
        mode: TravelMode,
        origin: LatLong,
        destination: LatLong,
        distance: AIO[Distance]
    ): AIO[((TravelMode, LatLong, LatLong), Distance)] = distance.map((mode, origin, destination) -> _)

    val combinedDirectedPaths: List[DirectedPath] =
      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup)

    // TODO: Replace by the syntax extension when this issue is closed: https://github.com/typelevel/cats/issues/2255
    Parallel
      .parFlatTraverse(combinedDirectedPaths) {
        case DirectedPath(origin, destination, travelModes) =>
          if (origin == destination) travelModes.traverse(mode => format(mode, origin, destination, AIO.pure(Distance.zero)))
          else travelModes.parTraverse(mode => format(mode, origin, destination, provider.distance(mode, origin, destination)))
      }
      .map(_.toMap)
  }
}

object DistanceApi {
  @inline def apply[AIO[_]: Async: Par](provider: DistanceProvider[AIO]): DistanceApi[AIO] = new DistanceApi(provider)

  private[DistanceApi] final val directedPathSemiGroup: Semigroup[DirectedPath] = new Semigroup[DirectedPath] {
    override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
      DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
  }

}
