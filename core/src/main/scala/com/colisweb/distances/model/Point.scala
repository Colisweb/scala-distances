package com.colisweb.distances.model

case class Point(latitude: Latitude, longitude: Longitude, elevation: Option[ElevationInMeters] = None) {

  def toRadians: Point = Point(math.toRadians(latitude), math.toRadians(longitude), elevation)

  def -(other: Point): Point = {
    val newElevation = for {
      thisElevation  <- elevation
      otherElevation <- other.elevation
    } yield thisElevation - otherElevation

    Point(latitude - other.latitude, longitude - other.longitude, newElevation)
  }
}
