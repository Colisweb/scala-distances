package com.colisweb.distances.model

import com.colisweb.distances.bird.Haversine

trait OriginDestination[-P] {
  def origin(path: P): Point
  def destination(path: P): Point
}

trait OriginDestinationData {
  def origin: Point
  def destination: Point

  lazy val birdDistanceInKm: DistanceInKm       = Haversine.distanceInKm(origin, destination)
  private lazy val birdDistanceInMeters: Double = birdDistanceInKm * 1000

  private lazy val elevationDifference: Option[Double] = for {
    destElev <- destination.elevation
    origElev <- origin.elevation
  } yield destElev - origElev

  lazy val elevationAngleInRadians: Double =
    if (origin == destination) 0d // otherwise we get NaN in atan
    else elevationDifference.map(elevDiff => math.atan(elevDiff / birdDistanceInMeters)).getOrElse(0d)

  lazy val elevationAngleInDegrees: Double = math.toDegrees(elevationAngleInRadians)
}

object OriginDestination {

  implicit val forData: OriginDestination[OriginDestinationData] =
    new OriginDestination[OriginDestinationData] {
      override def origin(path: OriginDestinationData): Point      = path.origin
      override def destination(path: OriginDestinationData): Point = path.destination
    }
}
