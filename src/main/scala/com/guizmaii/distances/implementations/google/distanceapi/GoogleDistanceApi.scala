package com.guizmaii.distances.implementations.google.distanceapi

import cats._
import cats.data._
import cats.implicits._
import com.google.maps.DirectionsApi.RouteRestriction
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}
import com.google.maps.model.{TrafficModel, TransitMode, Unit => GoogleDistanceUnit}
import com.guizmaii.distances.Types._
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{DistanceApi, Geocoder}
import monix.eval.Task
import monix.execution.CancelableFuture

final class GoogleDistanceApi(
    geoApiContext: GoogleGeoApiContext,
    override protected val alternativeCache: Option[GeoCache[(TravelMode, SerializableDistance)]] = None
) extends DistanceApi {

  import com.guizmaii.distances.utils.MonixSchedulers.AlwaysAsyncForkJoinScheduler._
  import com.guizmaii.distances.utils.RichImplicits._

//
  //override def distanceFromPostalCodesT(geocoder: Geocoder)(
  //    origin: PostalCode,
  //    destination: PostalCode
  //): Task[Distance] =
  //  if (origin == destination) Task.now(Distance.zero)
  //  else Task.zip2(geocoder.geocodeT(origin), geocoder.geocodeT(destination)).flatMap((distanceT _).tupled)
//
  //override def distance(origin: LatLong, destination: LatLong): CancelableFuture[Distance] =
  //  distanceT(origin, destination).runAsync
//
  //override def distanceFromPostalCodes(geocoder: Geocoder)(
  //    origin: PostalCode,
  //    destination: PostalCode
  //): CancelableFuture[Distance] = distanceFromPostalCodesT(geocoder)(origin, destination).runAsync
//
  //override def distancesT(paths: Seq[DirectedPath]): Task[Seq[DirectedPathWithDistance]] = Task.sequence {
  //  paths.map {
  //    case (origin, destination) =>
  //      if (origin == destination) Task.now((origin, destination, Distance.zero))
  //      else distanceT(origin, destination).map(distance => (origin, destination, distance))
  //  }
  //}
//
  //override def distances(paths: Seq[DirectedPath]): CancelableFuture[Seq[DirectedPathWithDistance]] =
  //  distancesT(paths).runAsync

  import TravelMode._

  override def distanceT(
      origin: LatLong,
      destination: LatLong,
      travelModes: List[TravelMode] = List(TravelMode.Driving)
  ): Task[Map[TravelMode, Distance]] = {
    def fetch(mode: TravelMode): Task[(TravelMode, SerializableDistance)] =
      DistanceMatrixApi
        .newRequest(geoApiContext.geoApiContext)
        .mode(mode.toGoogleTravelMode)
        .origins(origin.toGoogleLatLng)
        .destinations(destination.toGoogleLatLng)
        .units(GoogleDistanceUnit.METRIC)
        .toTask
        .map(res => mode -> res.rows.head.elements.head.asSerializableDistance)

    def fetchAndCache(mode: TravelMode): Task[(TravelMode, Distance)] = {
      val key = (mode, origin, destination)
      cache.getOrTask(key)(fetch(mode)).map { case (m, serializableDistance) => m -> Distance.apply(serializableDistance) }
    }

    if (origin == destination) Task.now(travelModes.map(_ -> Distance.zero).toMap)
    else travelModes.map(fetchAndCache).sequence.map(_.toMap)
  }

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
    //def fetch(mode: TravelMode): Task[(TravelMode, SerializableDistance)] =
    //  DistanceMatrixApi
    //    .newRequest(geoApiContext.geoApiContext)
    //    .mode(mode.toGoogleTravelMode)
    //    .origins(origin.toGoogleLatLng)
    //    .destinations(destination.toGoogleLatLng)
    //    .units(GoogleDistanceUnit.METRIC)
    //    .toTask
    //    .map(res => mode -> res.rows.head.elements.head.asSerializableDistance)

    def fetchAndCache(mode: TravelMode): Task[(TravelMode, Distance)] = {
      val key = (mode, origin, destination)
      cache.getOrTask(key)(fetch(mode)).map { case (m, serializableDistance) => m -> Distance.apply(serializableDistance) }
    }

    paths
      .distinctBy { case (origin, destination, t) => DirectedPath(origin, destination, t) }
      .map {
        case DirectedPath(origin, destination, travelModes) =>
          if (origin == destination) Task.now(travelModes.map(mode => (mode, origin, destination) -> Distance.zero).toMap)
          else {}
      }

    ???

  }

  override def distances(paths: List[DirectedPath]): CancelableFuture[Map[(TravelMode, LatLong, LatLong), Distance]] = ???

}

object GoogleDistanceApi {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleDistanceApi = new GoogleDistanceApi(geoApiContext)

  def apply(geoApiContext: GoogleGeoApiContext, geoCache: GeoCache[(TravelMode, SerializableDistance)]): GoogleDistanceApi =
    new GoogleDistanceApi(geoApiContext, Some(geoCache))
}
