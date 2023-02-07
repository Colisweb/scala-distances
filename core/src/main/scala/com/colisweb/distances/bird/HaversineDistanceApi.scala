package com.colisweb.distances.bird

import cats.Applicative
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{DistanceAndDuration, FixedSpeedTransportation, OriginDestination, PathResult}

class HaversineDistanceApi[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]
    extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P, segments: Int = 1): F[PathResult] = {
    val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
    val distanceAndDuration  = DistanceAndDuration(distanceInKilometers, timeInSeconds)
    Applicative[F].pure(PathResult(distanceAndDuration, List(DirectedPath(path.origin, path.destination))))
  }
}
