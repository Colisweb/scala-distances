package com.colisweb.distances.model.path

import java.time.Instant

import com.colisweb.distances.model._

object DirectedPathWithModeAndSpeedAt {
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithModeAndSpeedAt] = _.speed
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAndSpeedAt] = _.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAndSpeedAt]                       = _.departureTime
}

final case class DirectedPathWithModeAndSpeedAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    speed: SpeedInKmH,
    departureTime: Option[Instant]
) extends OriginDestinationData
