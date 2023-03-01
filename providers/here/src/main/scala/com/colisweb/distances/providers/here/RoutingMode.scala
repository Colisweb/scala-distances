package com.colisweb.distances.providers.here

import com.colisweb.distances.providers.here.HereRoutingProvider.Route

sealed trait RoutingMode {
  def best: List[Route] => Route
}
case object RoutingMode {
  case object MinimalDurationMode extends RoutingMode {
    override def best: List[Route] => Route =
      (results: List[Route]) => results.minBy(_.duration)
  }

  case object MinimalDistanceMode extends RoutingMode {
    override def best: List[Route] => Route =
      (results: List[Route]) => results.minBy(_.distance)
  }

}
