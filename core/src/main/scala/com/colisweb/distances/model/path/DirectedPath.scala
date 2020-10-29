package com.colisweb.distances.model.path

import java.time.Instant

import com.colisweb.distances.model._
import squants.motion.Velocity

final case class DirectedPath(origin: Point, destination: Point) extends OriginDestinationData

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
