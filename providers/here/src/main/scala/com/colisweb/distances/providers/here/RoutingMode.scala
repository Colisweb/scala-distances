package com.colisweb.distances.providers.here

import com.colisweb.distances.model.DistanceAndDuration

sealed trait RoutingMode {
  def mode: String
  def best: List[DistanceAndDuration] => DistanceAndDuration
}
case object RoutingMode {
  case object MinimalDurationMode extends RoutingMode {
    override def mode: String = "fast"

    override def best: List[DistanceAndDuration] => DistanceAndDuration = (results: List[DistanceAndDuration]) =>
      results.minBy(_.duration)
  }

  case object MinimalDistanceMode extends RoutingMode {
    override def mode: String = "short"

    override def best: List[DistanceAndDuration] => DistanceAndDuration = (results: List[DistanceAndDuration]) =>
      results.minBy(_.distance)
  }

}
