package com.colisweb.distances.providers.google

import java.time.Instant

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model._
import com.colisweb.distances.providers.google.GoogleDistanceProvider.RequestBuilder
import com.google.maps.model.{
  DistanceMatrix,
  DistanceMatrixElement,
  DistanceMatrixElementStatus,
  Unit => GoogleDistanceUnit
}
import com.google.maps.{DistanceMatrixApi, DistanceMatrixApiRequest}

class GoogleDistanceProvider[F[_]](
    googleContext: GoogleGeoApiContext,
    trafficModel: TrafficModel,
    requestExecutor: RequestExecutor[F]
)(implicit
    F: MonadError[F, Throwable]
) {
  import RequestBuilder._

  def singleRequest(
      travelMode: TravelMode,
      origin: Point,
      destination: Point,
      departureTime: Option[Instant]
  ): F[DistanceAndDuration] = {
    val request = RequestBuilder(googleContext, travelMode).withOriginDestination(origin, destination)
    for {
      requestMaybeWithTraffic <- requestWithPossibleTraffic(departureTime, request)
      response                <- requestExecutor.run(requestMaybeWithTraffic)
      distanceAndDuration     <- extractSingleResponse(response)
    } yield distanceAndDuration
  }

  private def requestWithPossibleTraffic(
      departureTime: Option[Instant],
      request: DistanceMatrixApiRequest
  ): F[DistanceMatrixApiRequest] = {
    departureTime match {
      case Some(time) if time.isBefore(Instant.now) => F.raiseError(PastTraffic(time))
      case Some(time)                               => request.withTraffic(trafficModel, time).pure[F]
      case None                                     => request.pure[F]
    }
  }

  private def extractSingleResponse(
      matrix: DistanceMatrix
  ): F[DistanceAndDuration] = {
    val element = matrix.rows(0).elements(0)
    extractMatrixResponseElement(element)
  }

  private def extractMatrixResponseElement(
      element: DistanceMatrixElement
  ): F[DistanceAndDuration] =
    element.status match {
      case DistanceMatrixElementStatus.OK =>
        val durationInSeconds: DurationInSeconds =
          Option(element.durationInTraffic).getOrElse(element.duration).inSeconds
        val distanceInKilometers: DistanceInKm = element.distance.inMeters.toDouble / 1000
        F.pure(DistanceAndDuration(distanceInKilometers, durationInSeconds))

      case DistanceMatrixElementStatus.NOT_FOUND =>
        F.raiseError(DistanceNotFound("origin and/or destination of this pairing could not be geocoded"))

      case DistanceMatrixElementStatus.ZERO_RESULTS =>
        F.raiseError(NoResults("no route could be found between the origin and destination"))

      case _ =>
        F.raiseError(UnknownGoogleError(element.toString))
    }
}

object GoogleDistanceProvider {

  object RequestBuilder {
    import GoogleModel._

    def apply(googleContext: GoogleGeoApiContext, travelMode: TravelMode): DistanceMatrixApiRequest =
      DistanceMatrixApi
        .newRequest(googleContext.geoApiContext)
        .mode(travelMode.asGoogle)
        .units(GoogleDistanceUnit.METRIC)

    implicit class RequestSingle(request: DistanceMatrixApiRequest) {
      def withOriginDestination(origin: Point, destination: Point): DistanceMatrixApiRequest =
        request
          .origins(origin.asGoogle)
          .destinations(destination.asGoogle)
    }

    implicit class RequestTraffic(request: DistanceMatrixApiRequest) {
      def withTraffic(trafficModel: TrafficModel, departureTime: Instant): DistanceMatrixApiRequest =
        request
          .trafficModel(trafficModel.asGoogle)
          .departureTime(departureTime)
    }
  }
}
