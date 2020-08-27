package com.colisweb.distances.model.path

import java.time.Instant

import com.colisweb.distances.model._

final case class DirectedPathWithModeAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    departureTime: Option[Instant]
) extends OriginDestinationData

object DirectedPathWithModeAt {
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAt] = _.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAt]                       = _.departureTime
}
