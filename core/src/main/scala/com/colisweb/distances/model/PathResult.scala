package com.colisweb.distances.model

final case class PathResult(
    distance: DistanceInKm,
    duration: DurationInSeconds,
    elevationProfile: Option[Double] = None
) {
  val speedInKmPerHour: SpeedInKmH   = distance / (duration / 3600)
  val speedInMetersPerSecond: Double = distance * 1000 / duration
}
