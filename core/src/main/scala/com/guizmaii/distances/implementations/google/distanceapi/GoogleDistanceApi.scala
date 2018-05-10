package com.guizmaii.distances.implementations.google.distanceapi

import cats.effect.Async
import cats.kernel.Semigroup
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.{Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types.{DirectedPath, _}
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{DistanceApi, Geocoder}

object GoogleDistanceApi {

  import TravelMode._
  import cats.implicits._
  import com.guizmaii.distances.utils.RichImplicits._

  private final val directedPathSemiGroup: Semigroup[DirectedPath] = new Semigroup[DirectedPath] {
    override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
      DirectedPath(origin = x.origin, destination = x.destination, (x.travelModes ++ y.travelModes).distinct)
  }

  def apply[AIO[_]](geoApiContext: GoogleGeoApiContext)(implicit AIO: Async[AIO]): DistanceApi[AIO] = new DistanceApi[AIO] {

    override def distance(origin: LatLong, destination: LatLong, travelModes: List[TravelMode]): AIO[Map[TravelMode, Distance]] =
      distances(DirectedPath(origin = origin, destination = destination, travelModes = travelModes) :: Nil)
        .map(_.map { case ((travelMode, _, _), distance) => travelMode -> distance })

    override def distanceFromPostalCodes(geocoder: Geocoder[AIO])(
        origin: PostalCode,
        destination: PostalCode,
        travelModes: List[TravelMode]
    ): AIO[Map[TravelMode, Distance]] =
      if (origin == destination) implicitly[Async[AIO]].pure(travelModes.map(_ -> Distance.zero).toMap)
      else
        (geocoder.geocodePostalCode(origin), geocoder.geocodePostalCode(destination))
          .mapN((_, _)) // TODO: Possible to use `parMapN` ??
          .flatMap { case (o, d) => distance(o, d, travelModes) }

    override def distances(paths: List[DirectedPath]): AIO[Map[(TravelMode, LatLong, LatLong), Distance]] = {
      def fetch(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[((TravelMode, LatLong, LatLong), Distance)] =
        DistanceMatrixApi
          .newRequest(geoApiContext.geoApiContext)
          .mode(mode.toGoogleTravelMode)
          .origins(origin.toGoogleLatLng)
          .destinations(destination.toGoogleLatLng)
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
