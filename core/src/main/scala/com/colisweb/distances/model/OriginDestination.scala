package com.colisweb.distances.model

import com.colisweb.distances.bird.Haversine
import com.colisweb.distances.model.path.Vector3D

trait OriginDestination[-P] {
  def origin(path: P): Point
  def destination(path: P): Point
}

trait OriginDestinationData {
  def origin: Point
  def destination: Point

  def descendingSlope: Option[Boolean] =
    for {
      originElevation      <- origin.elevation
      destinationElevation <- destination.elevation
    } yield destinationElevation < originElevation

  private lazy val vector: Vector3D              = Vector3D(destination - origin)
  private lazy val vectorSameElevation: Vector3D = Vector3D(destination.copy(elevation = origin.elevation) - origin)

  lazy val elevationAngleInRadians: Double = vectorSameElevation.angleInRadians(vector)
  lazy val elevationAngleInDegrees: Double = vectorSameElevation.angleInDegrees(vector)

  lazy val birdDistanceInKm: DistanceInKm = Haversine.distanceInKm(origin, destination)
}

object OriginDestination {

  implicit val forData: OriginDestination[OriginDestinationData] =
    new OriginDestination[OriginDestinationData] {
      override def origin(path: OriginDestinationData): Point      = path.origin
      override def destination(path: OriginDestinationData): Point = path.destination
    }
}
