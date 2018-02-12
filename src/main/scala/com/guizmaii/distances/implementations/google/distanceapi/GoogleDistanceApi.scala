package com.guizmaii.distances.implementations.google.distanceapi

import cats.implicits._
import cats.kernel.Semigroup
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.{Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types.{DirectedPath, _}
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{DistanceApi, Geocoder}
import monix.eval.Task
import monix.execution.CancelableFuture

final class GoogleDistanceApi(geoApiContext: GoogleGeoApiContext) extends DistanceApi {

  import TravelMode._
  import com.guizmaii.distances.utils.RichImplicits._
  import monix.execution.Scheduler.Implicits.global

  override def distanceT(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  )(implicit cache: GeoCache[CacheableDistance]): Task[Map[TravelMode, Distance]] =
    distancesT(DirectedPath(origin = origin, destination = destination, travelModes = travelModes) :: Nil)
      .map(_.map { case ((travelMode, _, _), distance) => travelMode -> distance })

  override def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  )(implicit cache: GeoCache[CacheableDistance]): CancelableFuture[Map[TravelMode, Distance]] =
    distanceT(origin, destination, travelModes).runAsync

  override def distanceFromPostalCodesT(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): Task[Map[TravelMode, Distance]] = {
    if (origin == destination) Task.now(travelModes.map(_ -> Distance.zero).toMap)
    else
      Task
        .zip2(geocoder.geocodePostalCodeT(origin), geocoder.geocodePostalCodeT(destination))
        .flatMap { case (o, d) => distanceT(o, d, travelModes) }
  }

  override def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  )(implicit cache: GeoCache[CacheableDistance], geoCache: GeoCache[LatLong]): CancelableFuture[Map[TravelMode, Distance]] =
    distanceFromPostalCodesT(geocoder)(origin, destination, travelModes).runAsync

  private[this] val directedPathSemiGroup: Semigroup[DirectedPath] = new Semigroup[DirectedPath] {
    override def combine(x: DirectedPath, y: DirectedPath): DirectedPath =
      DirectedPath(origin = x.origin, destination = x.destination, x.travelModes ++ y.travelModes)
  }

  override def distancesT(paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): Task[Map[(TravelMode, LatLong, LatLong), Distance]] = {
    def fetch(mode: TravelMode, origin: LatLong, destination: LatLong): Task[((TravelMode, LatLong, LatLong), SerializableDistance)] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(mode.toGoogleTravelMode)
        .origins(origin.toGoogleLatLng)
        .destinations(destination.toGoogleLatLng)
        .units(GoogleDistanceUnit.METRIC)
        .toTask
        .map(res => (mode, origin, destination) -> res.rows.head.elements.head.asSerializableDistance)

    def fetchAndCache(mode: TravelMode, origin: LatLong, destination: LatLong): Task[((TravelMode, LatLong, LatLong), Distance)] = {
      val key = (mode, origin, destination)

      cache
        .getOrTask(key)(fetch(mode, origin, destination))
        .map { case (tuple, serializableDistance) => tuple -> Distance.apply(serializableDistance) }
    }

    paths
      .filter(_.travelModes.isEmpty)
      .combineDuplicatesOn { case DirectedPath(origin, destination, _) => (origin, destination) }(directedPathSemiGroup)
      .flatMap {
        case DirectedPath(origin, destination, travelModes) =>
          travelModes.map(
            mode =>
              if (origin == destination) Task.now((mode, origin, destination) -> Distance.zero)
              else fetchAndCache(mode, origin, destination))
      }
      .sequence
      .map(_.toMap)
  }

  override def distances(paths: List[DirectedPath])(
      implicit cache: GeoCache[CacheableDistance]): CancelableFuture[Map[(TravelMode, LatLong, LatLong), Distance]] =
    distancesT(paths).runAsync

}

object GoogleDistanceApi {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleDistanceApi = new GoogleDistanceApi(geoApiContext)
}
