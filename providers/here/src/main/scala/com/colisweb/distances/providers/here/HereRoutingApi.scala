package com.colisweb.distances.providers.here

import cats.effect.Concurrent
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model._

class HereRoutingApi[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
    hereRoutingProvider: HereRoutingProvider[F]
) extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[DistanceAndDuration] = {
    hereRoutingProvider.singleRequest(
      path.origin,
      path.destination,
      path.departureTime,
      path.travelMode
    )
  }

}

object HereRoutingApi {
  def async[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      hereContext: HereRoutingContext
  )(chooseBestRoute: RoutingMode)(implicit C: Concurrent[F]) = {
    val hereProvider = new HereRoutingProvider(hereContext)(chooseBestRoute)
    new HereRoutingApi[F, P](hereProvider)
  }
}
