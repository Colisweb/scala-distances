package com.colisweb.distances.providers.google

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model._
import com.colisweb.distances.providers.google.GoogleDistanceDirectionsProvider.RequestBuilder._
import com.colisweb.distances.providers.google.GoogleDistanceDirectionsProvider._
import com.google.maps.model.{DirectionsRoute, Unit => GoogleDistanceUnit}
import com.google.maps.{DirectionsApi, DirectionsApiRequest}

import java.time.Instant

class GoogleDistanceDirectionsProvider[F[_]](
    googleContext: GoogleGeoApiContext,
    trafficModel: TrafficModel,
    requestExecutor: RequestExecutor[F]
)(chooseBestRoute: List[DirectionsRoute] => DirectionsRoute)(implicit
    F: MonadError[F, Throwable]
) {

  def singleRequest(
      travelMode: TravelMode,
      origin: Point,
      destination: Point,
      departureTime: Option[Instant]
  ): F[PathResult] = {

    val request = RequestBuilder(googleContext, travelMode).withOriginDestination(origin, destination)
    for {
      requestMaybeWithTraffic <- requestWithPossibleTraffic(departureTime, request)
      response                <- requestExecutor.run(requestMaybeWithTraffic)
      _ <- F.whenA(response.routes.isEmpty)(
        F.raiseError(NoResults("no route could be found between the origin and destination"))
      )
      bestRoute            = chooseBestRoute(response.routes.toList)
      distancesAndDuration = extractResponse(bestRoute)
      (distance, duration) = distancesAndDuration
    } yield PathResult(distance, duration, Nil)

  }

  private def extractResponse(route: DirectionsRoute): (DistanceInKm, DurationInSeconds) = {
    val totalLegDuration = route.legs.map(e => Option(e.durationInTraffic).getOrElse(e.duration).inSeconds).sum
    val totalLegDistance = route.legs.map(e => e.distance.inMeters.toDouble / 1000).sum
    (totalLegDistance, totalLegDuration)
  }

  private def requestWithPossibleTraffic(
      departureTime: Option[Instant],
      request: DirectionsApiRequest
  ): F[DirectionsApiRequest] = {
    departureTime match {
      case Some(time) if time.isBefore(Instant.now) => F.raiseError(PastTraffic(time))
      case Some(time)                               => request.withTraffic(trafficModel, time).pure[F]
      case None                                     => request.pure[F]
    }
  }
}
object GoogleDistanceDirectionsProvider {

  def chooseMinimalDistanceRoute(routes: List[DirectionsRoute]): DirectionsRoute =
    routes.minBy(_.legs.map(e => e.distance.inMeters).sum)

  def chooseMinimalDurationRoute(routes: List[DirectionsRoute]): DirectionsRoute =
    routes.minBy(_.legs.map(e => Option(e.durationInTraffic).getOrElse(e.duration).inSeconds).sum)

  object RequestBuilder {

    import GoogleModel._

    def apply(googleContext: GoogleGeoApiContext, travelMode: TravelMode): DirectionsApiRequest =
      DirectionsApi
        .newRequest(googleContext.geoApiContext)
        .mode(travelMode.asGoogle)
        .units(GoogleDistanceUnit.METRIC)
        .alternatives(true)

    implicit class RequestSingle(request: DirectionsApiRequest) {
      def withOriginDestination(origin: Point, destination: Point): DirectionsApiRequest =
        request
          .origin(origin.asGoogle)
          .destination(destination.asGoogle)
    }

    implicit class RequestTraffic(request: DirectionsApiRequest) {
      def withTraffic(trafficModel: TrafficModel, departureTime: Instant): DirectionsApiRequest =
        request
          .trafficModel(trafficModel.asGoogle)
          .departureTime(departureTime)
    }

  }

}
