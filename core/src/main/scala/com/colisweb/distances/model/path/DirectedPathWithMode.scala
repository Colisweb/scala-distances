package com.colisweb.distances.model.path

import com.colisweb.distances.model.{OriginDestinationData, Point, TravelMode, TravelModeTransportation}

final case class DirectedPathWithMode(origin: Point, destination: Point, travelMode: TravelMode)
    extends OriginDestinationData

object DirectedPathWithMode {
  implicit val travelModeTransportation: TravelModeTransportation[DirectedPathWithMode] = _.travelMode
}
