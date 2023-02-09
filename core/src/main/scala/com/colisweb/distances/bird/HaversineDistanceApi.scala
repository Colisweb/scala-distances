package com.colisweb.distances.bird

import cats.Applicative
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{FixedSpeedTransportation, OriginDestination, PathResult}

class HaversineDistanceApi[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]
    extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[PathResult] = {
    val distanceInKilometers = Haversine.distanceInKm(path.origin, path.destination)
    val timeInSeconds        = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
    Applicative[F].pure(
      PathResult(distanceInKilometers, timeInSeconds, List(DirectedPath(path.origin, path.destination)))
    )
  }
}
