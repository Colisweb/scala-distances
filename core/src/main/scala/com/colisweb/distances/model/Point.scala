package com.colisweb.distances.model

case class Point(latitude: Latitude, longitude: Longitude) {

  def toRadians: Point = Point(math.toRadians(latitude), math.toRadians(longitude))
}
