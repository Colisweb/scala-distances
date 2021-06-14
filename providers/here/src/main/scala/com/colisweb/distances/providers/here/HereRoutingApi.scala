package com.colisweb.distances.providers.here

import cats.effect.IO
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{DepartureTime, DistanceAndDuration, OriginDestination, Point, TravelModeTransportation}

import java.time.{Duration, ZonedDateTime}
import com.colisweb.distances.model.syntax._

class HereRoutingApi[F[_], P: OriginDestination: TravelModeTransportation: DepartureTime] extends DistanceApi[F, P] {
  override def distance(path: P): F[DistanceAndDuration] = {

  }


  private def aCall(data: P, departure: ZonedDateTime): IO[DistanceAndDuration] = {
    val query: Map[String, String] = Map(
      "transportMode" -> "car",
      "origin"        -> s"${data.origin.latitude},${data.origin.longitude}",
      "destination"   -> s"${data.destination.latitude},${data.destination.longitude}",
      "departureTime" -> departure.toString,
      "return"        -> "summary",
      "apiKey"        -> apiKey,
      "routingMode" -> "short"
    )
    for {
      before <- IO.pure(ZonedDateTime.now())
      response <- IO.delay(
        requests.get(
          url = baseUrl,
          headers = List(("Accept", "application/json")),
          params = query.toList,
          connectTimeout = 1000,
          readTimeout = 1000,
          check = false
        )
      )
      after <- IO.pure(ZonedDateTime.now())
      result <- response match {
        case res if res.is2xx =>
          decode[Response](res.text()) match {
            case Left(err) => IO.raiseError(new RuntimeException(err))
            case Right(r) =>
              val distances            = r.routes.map(_.sections.map(_.summary.length).sum / 1000)
              val durations            = r.routes.map(_.sections.map(_.summary.duration).sum)
              val indexForShortestPath = distances.zipWithIndex.min._2
              IO.pure(
                Result(
                  origin,
                  destination,
                  durations(indexForShortestPath),
                  distances(indexForShortestPath),
                  Duration.between(before, after)
                )
              )
          }
        case err => IO.raiseError(new RuntimeException(s"it failed: ${err.statusCode}"))
      }
    } yield result

  }

}
