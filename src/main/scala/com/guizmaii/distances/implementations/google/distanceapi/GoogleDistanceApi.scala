package com.guizmaii.distances.implementations.google.distanceapi

import com.google.maps.DistanceMatrixApi
import com.guizmaii.distances.Types._
import com.guizmaii.distances.implementations.cache.GeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{DistanceApi, Geocoder}
import monix.eval.Task
import monix.execution.CancelableFuture

import scala.collection.immutable.Seq

final class GoogleDistanceApi(
    geoApiContext: GoogleGeoApiContext,
    override protected val alternativeCache: Option[GeoCache[SerializableDistance]] = None
) extends DistanceApi {

  import com.guizmaii.distances.utils.MonixSchedulers.AlwaysAsyncForkJoinScheduler._
  import com.guizmaii.distances.utils.RichImplicits._

  private def toGoogleRepresentation(latLong: LatLong): String = s"${latLong.latitude},${latLong.longitude}"

  override def distanceT(origin: LatLong, destination: LatLong): Task[Distance] = {
    def fetch: Task[SerializableDistance] =
      DistanceMatrixApi
        .getDistanceMatrix(
          geoApiContext.geoApiContext,
          Array(toGoogleRepresentation(origin)),
          Array(toGoogleRepresentation(destination))
        )
        .toTask
        .map(_.rows.head.elements.head.asSerializableDistance)

    val key = origin -> destination
    cache
      .getOrTask(key)(fetch)
      .map(Distance.apply)
  }

  override def distanceFromPostalCodesT(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode
  ): Task[Distance] =
    if (origin == destination) geocoder.geocodeT(origin).flatMap(latLong => distanceT(latLong, latLong))
    else Task.zip2(geocoder.geocodeT(origin), geocoder.geocodeT(destination)).flatMap((distanceT _).tupled)

  override def distance(origin: LatLong, destination: LatLong): CancelableFuture[Distance] =
    distanceT(origin, destination).runAsync

  override def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode
  ): CancelableFuture[Distance] = distanceFromPostalCodesT(geocoder)(origin, destination).runAsync

  override def distancesT(paths: Seq[DirectedPath]): Task[Seq[DirectedPathWithDistance]] = Task.sequence {
    paths.map {
      case (origin, destination) =>
        if (origin == destination) Task.now((origin, destination, Distance.zero))
        else distanceT(origin, destination).map(distance => (origin, destination, distance))
    }
  }

  override def distances(paths: Seq[DirectedPath]): CancelableFuture[Seq[DirectedPathWithDistance]] =
    distancesT(paths).runAsync

}

object GoogleDistanceApi {
  def apply(geoApiContext: GoogleGeoApiContext): GoogleDistanceApi = new GoogleDistanceApi(geoApiContext)

  def apply(geoApiContext: GoogleGeoApiContext, geoCache: GeoCache[SerializableDistance]): GoogleDistanceApi =
    new GoogleDistanceApi(geoApiContext, Some(geoCache))
}
