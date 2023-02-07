package com.colisweb.distances.providers.here

import cats.MonadError
import cats.effect.Sync
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model._

class HereRoutingApi[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
    hereRoutingProvider: HereRoutingProvider[F]
) extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P, segments: Int = 1): F[PathResult] =
    hereRoutingProvider.singleRequest(
      origin = path.origin,
      destination = path.destination,
      departure = path.departureTime,
      travelMode = path.travelMode,
      segments = segments
    )

}

object HereRoutingApi {
  def async[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      hereContext: HereRoutingContext,
      excludeCountriesIso: List[String] = Nil
  )(chooseBestRoute: RoutingMode)(implicit C: Sync[F]): HereRoutingApi[F, P] = {
    val hereProvider =
      new HereRoutingProvider(hereContext, new AsyncRequestExecutor[F]())(chooseBestRoute, excludeCountriesIso)
    new HereRoutingApi[F, P](hereProvider)
  }

  def sync[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime](
      hereContext: HereRoutingContext,
      excludeCountriesIso: List[String] = Nil
  )(chooseBestRoute: RoutingMode)(implicit C: MonadError[F, Throwable]): HereRoutingApi[F, P] = {
    val hereProvider =
      new HereRoutingProvider(hereContext, new SyncRequestExecutor[F]())(chooseBestRoute, excludeCountriesIso)
    new HereRoutingApi[F, P](hereProvider)
  }
}
