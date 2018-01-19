package com.guizmaii.distances.implementations.google.distanceapi

import cats.implicits._
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.{Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types._
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{DistanceApi, Geocoder}
import monix.eval.Task
import monix.execution.CancelableFuture

import scala.collection.mutable.{Map => MutableMap}

final class GoogleDistanceApi(
    geoApiContext: GoogleGeoApiContext,
    override protected val alternativeCache: Option[GeoCache[(TravelMode, SerializableDistance)]] = None
) extends DistanceApi {

  import TravelMode._
  import com.guizmaii.distances.utils.MonixSchedulers.AlwaysAsyncForkJoinScheduler._
  import com.guizmaii.distances.utils.RichImplicits._

  override def distanceT(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  ): Task[Map[TravelMode, Distance]] =
    distancesT(DirectedPath(origin = origin, destination = destination, travelModes = travelModes) :: Nil)
      .map(_.map { case ((travelMode, _, _), distance) => travelMode -> distance })

  override def distance(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  ): CancelableFuture[Map[TravelMode, Distance]] = distanceT(origin, destination, travelModes).runAsync

  override def distanceFromPostalCodesT(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  ): Task[Map[TravelMode, Distance]] = {
    if (origin == destination) Task.now(travelModes.map(_ -> Distance.zero).toMap)
    else
      Task
        .zip2(geocoder.geocodeT(origin), geocoder.geocodeT(destination))
        .flatMap { case (o, d) => distanceT(o, d, travelModes) }
  }

  override def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  ): CancelableFuture[Map[TravelMode, Distance]] =
    distanceFromPostalCodesT(geocoder)(origin, destination, travelModes).runAsync

  override def distancesT(paths: List[DirectedPath]): Task[Map[(TravelMode, LatLong, LatLong), Distance]] = {
    def fetch(mode: TravelMode, origin: LatLong, destination: LatLong): Task[(TravelMode, SerializableDistance)] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(mode.toGoogleTravelMode)
        .origins(origin.toGoogleLatLng)
        .destinations(destination.toGoogleLatLng)
        .units(GoogleDistanceUnit.METRIC)
        .toTask
        .map(res => mode -> res.rows.head.elements.head.asSerializableDistance)

    def fetchAndCache(mode: TravelMode, origin: LatLong, destination: LatLong): Task[((TravelMode, LatLong, LatLong), Distance)] = {
      val key = (mode, origin, destination)

      cache
        .getOrTask(key)(fetch(mode, origin, destination))
        .map { case (m, serializableDistance) => ((m, origin, destination), Distance.apply(serializableDistance)) }
    }

    def distinctPathsWithTravelModeAccumulation(duplicatedPaths: List[DirectedPath]): Vector[((LatLong, LatLong), List[TravelMode])] = {
      val zero: MutableMap[(LatLong, LatLong), List[TravelMode]] = MutableMap().withDefaultValue(List.empty)

      duplicatedPaths
        .foldLeft(zero) {
          case (res, DirectedPath(origin, destination, travelModes)) =>
            res((origin, destination)) = res((origin, destination)) ++ travelModes
            res
        }
        .toVector
    }

    distinctPathsWithTravelModeAccumulation(paths.filter(_.travelModes.isEmpty))
      .flatMap {
        case ((origin, destination), travelModes) =>
          travelModes.map(
            mode =>
              if (origin == destination) Task.now((mode, origin, destination) -> Distance.zero)
              else fetchAndCache(mode, origin, destination))
      }
      .sequence
      .map(_.toMap)
  }

  override def distances(paths: List[DirectedPath]): CancelableFuture[Map[(TravelMode, LatLong, LatLong), Distance]] =
    distancesT(paths).runAsync

}

object GoogleDistanceApi {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleDistanceApi = new GoogleDistanceApi(geoApiContext)

  def apply(geoApiContext: GoogleGeoApiContext, geoCache: GeoCache[(TravelMode, SerializableDistance)]): GoogleDistanceApi =
    new GoogleDistanceApi(geoApiContext, Some(geoCache))
}
