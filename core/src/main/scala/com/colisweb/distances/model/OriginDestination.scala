package com.colisweb.distances.model

import com.colisweb.distances.model.path.Vector

trait OriginDestination[-P] {
  def origin(path: P): Point
  def destination(path: P): Point
}

trait OriginDestinationData {
  def origin: Point
  def destination: Point

  lazy val vector: Vector                      = Vector(destination - origin)
  private lazy val vectorSameElevation: Vector = Vector(destination.copy(elevation = origin.elevation) - origin)

  lazy val elevationAngleInRadians: Double = vectorSameElevation.angleInRadians(vector)
  lazy val elevationAngleInDegrees: Double = vectorSameElevation.angleInDegrees(vector)
}

object OriginDestination {

  implicit val forData: OriginDestination[OriginDestinationData] =
    new OriginDestination[OriginDestinationData] {
      override def origin(path: OriginDestinationData): Point      = path.origin
      override def destination(path: OriginDestinationData): Point = path.destination
    }
}
