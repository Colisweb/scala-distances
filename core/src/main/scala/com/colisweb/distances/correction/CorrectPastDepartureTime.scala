package com.colisweb.distances.correction

import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.syntax._
import com.colisweb.distances.model.{DepartureTime, DepartureTimeUpdatable, DistanceAndDuration}

import java.time.{Clock, Instant}
import scala.concurrent.duration.FiniteDuration

case class CorrectPastDepartureTime[F[_], P: DepartureTime: DepartureTimeUpdatable](
    api: DistanceApi[F, P],
    margin: FiniteDuration,
    clock: Clock
) extends DistanceApi[F, P] {
  override def distance(path: P): F[DistanceAndDuration] = {
    val threshold = Instant.now(clock).plusSeconds(margin.toSeconds)
    if (path.departureTime.exists(_.isBefore(threshold)))
      api.distance(path.updatedDepartureTime(threshold))
    else
      api.distance(path)
  }
}
