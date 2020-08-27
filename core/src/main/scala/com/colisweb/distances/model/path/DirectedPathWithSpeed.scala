package com.colisweb.distances.model.path

import com.colisweb.distances.model.{FixedSpeedTransportation, OriginDestinationData, Point, SpeedInKmH}

final case class DirectedPathWithSpeed(origin: Point, destination: Point, speed: SpeedInKmH)
    extends OriginDestinationData

object DirectedPathWithSpeed {
  implicit val fixedSpeedTransportation: FixedSpeedTransportation[DirectedPathWithSpeed] = _.speed
}
