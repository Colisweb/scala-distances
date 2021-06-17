package com.colisweb.distances.model.path

import com.colisweb.distances.model._

import java.time.Instant

final case class DirectedPath(origin: Point, destination: Point) extends OriginDestinationData

object DirectedPath {

  def apply(origin: Point, destination: Point, travelMode: TravelMode): DirectedPathWithMode =
    DirectedPathWithMode(origin, destination, travelMode)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAt =
    DirectedPathWithModeAt(origin, destination, travelMode, departureTime)
}
