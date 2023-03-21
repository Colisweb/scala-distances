package com.colisweb.distances.bird

import cats.Applicative
import com.colisweb.distances.DistanceApi
import com.colisweb.distances.model.{FixedSpeedTransportation, OriginDestination, PathResult}

class HaversineDistanceApi[F[_]: Applicative, P: OriginDestination: FixedSpeedTransportation]
    extends DistanceApi[F, P] {
  import com.colisweb.distances.model.syntax._

  override def distance(path: P): F[PathResult] = {
    val distanceInKilometers         = Haversine.distanceInKm(path.origin, path.destination)
    val timeInSeconds                = DurationFromSpeed.durationForDistance(distanceInKilometers, path.speed)
    val rollingResistanceCoefficient = 0.0125
    // we don't know the elevation so only the distance is taken into account
    val elevationProfile = distanceInKilometers * 1000 * rollingResistanceCoefficient
    Applicative[F].pure(PathResult(distanceInKilometers, timeInSeconds, Some(elevationProfile)))
  }
}
