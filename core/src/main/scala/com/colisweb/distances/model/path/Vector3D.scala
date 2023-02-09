package com.colisweb.distances.model.path

import com.colisweb.distances.model.{ElevationInMeters, Latitude, Longitude, Point}

final case class Vector3D(coordinates: Point) {

  lazy val magnitude: Double =
    math.sqrt(
      coordinates.latitude * coordinates.latitude +
        coordinates.longitude * coordinates.longitude +
        coordinates.elevation.map(e => e * e).getOrElse(0d)
    )

  lazy val normed: Vector3D =
    Vector3D(
      Point(
        latitude = coordinates.latitude / magnitude,
        longitude = coordinates.longitude / magnitude,
        elevation = coordinates.elevation.map(_ / magnitude)
      )
    )

  def dotProduct(other: Vector3D): Double = {
    val elevationTerm = for {
      thisNormedElevation  <- normed.coordinates.elevation
      otherNormedElevation <- other.normed.coordinates.elevation
    } yield thisNormedElevation * otherNormedElevation

    normed.coordinates.latitude * other.normed.coordinates.latitude +
      normed.coordinates.longitude * other.normed.coordinates.longitude +
      elevationTerm.getOrElse(0d)
  }

  def angleInRadians(other: Vector3D): Double = math.acos(dotProduct(other))

  def angleInDegrees(other: Vector3D): Double = math.toDegrees(angleInRadians(other))
}

object Vector3D {
  def apply(lat: Latitude, lon: Longitude, elevation: ElevationInMeters): Vector3D =
    Vector3D(Point(lat, lon, Some(elevation)))
}
