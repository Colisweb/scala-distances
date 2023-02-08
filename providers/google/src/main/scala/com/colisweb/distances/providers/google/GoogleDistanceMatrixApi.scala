package com.colisweb.distances.providers.google

import cats.MonadError
import cats.effect.Concurrent
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DepartureTime, OriginDestination, PathResult, TravelModeTransportation}

class GoogleDistanceMatrixApi[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
    provider: GoogleDistanceMatrixProvider[F]
) extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[PathResult] =
    provider.singleRequest(path.travelMode, path.origin, path.destination, path.departureTime)
}

object GoogleDistanceMatrixApi {
  def sync[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(implicit
      F: MonadError[F, Throwable]
  ): GoogleDistanceMatrixApi[F, P] = {
    val executor = new SyncRequestExecutor[F]
    val provider = new GoogleDistanceMatrixProvider(googleContext, trafficModel, executor)
    new GoogleDistanceMatrixApi[F, P](provider)
  }

  def async[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      googleContext: GoogleGeoApiContext,
      trafficModel: TrafficModel
  )(implicit
      F: Concurrent[F]
  ): GoogleDistanceMatrixApi[F, P] = {
    val executor = new AsyncRequestExecutor[F]
    val provider = new GoogleDistanceMatrixProvider(googleContext, trafficModel, executor)
    new GoogleDistanceMatrixApi[F, P](provider)
  }

}
