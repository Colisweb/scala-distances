package com.colisweb.distances.model.path

import com.colisweb.distances.model._

final case class DirectedPathWithMode(origin: Point, destination: Point, travelMode: TravelMode)
    extends OriginDestinationData

object DirectedPathWithMode {
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithMode] = _.travelMode
  implicit val travelModeSpeed: FixedSpeedTransportation[DirectedPathWithMode]          = _.travelMode.maxSpeed
}
