package com.colisweb.distances.model

import java.time.Instant

import squants.motion.Velocity

final case class DirectedPath(origin: Point, destination: Point) extends OriginDestinationData

final case class DirectedPathWithSpeed(origin: Point, destination: Point, speed: SpeedInKmH)
    extends OriginDestinationData

final case class DirectedPathWithMode(origin: Point, destination: Point, travelMode: TravelMode)
    extends OriginDestinationData

final case class DirectedPathWithModeAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    departureTime: Option[Instant]
) extends OriginDestinationData

final case class DirectedPathWithModeAndSpeedAt(
    origin: Point,
    destination: Point,
    travelMode: TravelMode,
    speed: SpeedInKmH,
    departureTime: Option[Instant]
) extends OriginDestinationData

object DirectedPath {

  def apply(origin: Point, destination: Point, speed: SpeedInKmH): DirectedPathWithSpeed =
    DirectedPathWithSpeed(origin, destination, speed)

  def apply(origin: Point, destination: Point, speed: Velocity): DirectedPathWithSpeed =
    DirectedPathWithSpeed(origin, destination, speed.toKilometersPerHour)

  def apply(origin: Point, destination: Point, travelMode: TravelMode): DirectedPathWithMode =
    DirectedPathWithMode(origin, destination, travelMode)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAt =
    DirectedPathWithModeAt(origin, destination, travelMode, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: SpeedInKmH,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAndSpeedAt =
    DirectedPathWithModeAndSpeedAt(origin, destination, travelMode, speed, departureTime)

  def apply(
      origin: Point,
      destination: Point,
      travelMode: TravelMode,
      speed: Velocity,
      departureTime: Option[Instant]
  ): DirectedPathWithModeAndSpeedAt =
    DirectedPathWithModeAndSpeedAt(origin, destination, travelMode, speed.toKilometersPerHour, departureTime)

}

object DirectedPathWithSpeed {
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithSpeed] =
    (path: DirectedPathWithSpeed) => path.speed
}

object DirectedPathWithMode {
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithMode] =
    _.travelMode
}

object DirectedPathWithModeAt {
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAt] =
    _.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAt] =
    _.departureTime
}

object DirectedPathWithModeAndSpeedAt {
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithModeAndSpeedAt] =
    _.speed
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithModeAndSpeedAt] =
    _.travelMode
  implicit val departureTime: DepartureTime[DirectedPathWithModeAndSpeedAt] =
    _.departureTime
}
