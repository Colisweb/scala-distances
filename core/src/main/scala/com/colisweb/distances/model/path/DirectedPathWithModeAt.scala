package com.colisweb.distances.model.path

import com.colisweb.distances.model._

import java.time.Instant

final case class DirectedPathWithModeAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    departureTime: Option[Instant]
) extends OriginDestinationData

object DirectedPathWithModeAt {

  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAt] = _.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAt]                       = _.departureTime
  implicit val travelModeSpeed: FixedSpeedTransportation[DirectedPathWithModeAt]          = _.travelMode.maxSpeed
  implicit val departureTimeUpdatable: DepartureTimeUpdatable[DirectedPathWithModeAt] =
    (path: DirectedPathWithModeAt, departureTime: Instant) => path.copy(departureTime = Some(departureTime))
}
