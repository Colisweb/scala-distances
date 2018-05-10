package com.guizmaii.distances

import cats.effect.Async
import cats.kernel.Semigroup
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.{Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types.{DirectedPath, LatLong, _}
import com.guizmaii.distances.utils.GoogleGeoApiContext

abstract class DistanceProvider[AIO[_]: Async] {

  def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]]

}

object GoogleDistanceProvider {

  import TravelMode._
  import cats.implicits._
  import cats.temp.par._
  import com.guizmaii.distances.utils.RichImplicits._

  private final val directedPathSemiGroup: Semigroup[DirectedPath] = new Semigroup[DirectedPath] {
    override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
      DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
  }

  def apply[AIO[_]: Par](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): DistanceProvider[AIO] = new DistanceProvider[AIO] {

    override def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]] = {
      def fetch(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[((TravelMode, LatLong, LatLong), Distance)] =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(mode.asGoogleTravelMode)
          .origins(origin.asGoogleLatLng)
          .destinations(destination.asGoogleLatLng)
          .units(GoogleDistanceUnit.METRIC)
          .asEffect
          .map(res => (mode, origin, destination) -> res.rows.head.elements.head.asDistance)

      paths
        .filter(_.travelModes.nonEmpty)
        .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup)
        .flatMap {
          case DirectedPath(origin, destination, travelModes) =>
            travelModes.map(
              mode =>
                if (origin == destination) AIO.pure((mode, origin, destination) -> Distance.zero)
                else fetch(mode, origin, destination))
        }
        .sequence
        .map(_.toMap)
    }
  }
}
