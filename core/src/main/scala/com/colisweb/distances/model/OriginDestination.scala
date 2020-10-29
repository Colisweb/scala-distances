package com.colisweb.distances.model

trait OriginDestination[-P] {
  def origin(path: P): Point
  def destination(path: P): Point
}

trait OriginDestinationData {
  def origin: Point
  def destination: Point
}

object OriginDestination {

  implicit val forData: OriginDestination[OriginDestinationData] =
    new OriginDestination[OriginDestinationData] {
      override def origin(path: OriginDestinationData): Point      = path.origin
      override def destination(path: OriginDestinationData): Point = path.destination
    }
}
