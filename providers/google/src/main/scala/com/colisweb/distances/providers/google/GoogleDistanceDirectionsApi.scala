package com.colisweb.distances.providers.google

import cats.MonadError
import cats.effect.Concurrent
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DepartureTime, OriginDestination, PathResult, TravelModeTransportation}
import com.google.maps.model.DirectionsRoute

class GoogleDistanceDirectionsApi[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
    provider: GoogleDistanceDirectionsProvider[F]
) extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[PathResult] =
    provider.singleRequest(path.travelMode, path.origin, path.destination, path.departureTime)
}

object GoogleDistanceDirectionsApi {
  def sync[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(chooseBestRoute: List[DirectionsRoute] => DirectionsRoute)(implicit
      F: MonadError[F, Throwable]
  ): GoogleDistanceDirectionsApi[F, P] = {
    val executor = new SyncRequestExecutor[F]
    val provider = new GoogleDistanceDirectionsProvider(googleContext, trafficModel, executor)(chooseBestRoute)
    new GoogleDistanceDirectionsApi[F, P](provider)
  }

  def async[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(chooseBestRoute: List[DirectionsRoute] => DirectionsRoute)(implicit
      F: Concurrent[F]
  ): GoogleDistanceDirectionsApi[F, P] = {
    val executor = new AsyncRequestExecutor[F]
    val provider = new GoogleDistanceDirectionsProvider(googleContext, trafficModel, executor)(chooseBestRoute)
    new GoogleDistanceDirectionsApi[F, P](provider)
  }

}
