package com.colisweb.distances.model

// todo: unit of elevation in Here?
case class Point(latitude: Latitude, longitude: Longitude, elevation: Option[Double] = None) {

  def toRadians: Point = Point(math.toRadians(latitude), math.toRadians(longitude))

  def -(other: Point): Point = {
    val newElevation = for {
      thisElevation  <- elevation
      otherElevation <- other.elevation
    } yield thisElevation - otherElevation

    Point(latitude - other.latitude, longitude - other.longitude, newElevation)
  }
}
