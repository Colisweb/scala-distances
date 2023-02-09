package com.colisweb.distances.model

final case class PathResult(distance: DistanceInKm, duration: DurationInSeconds, paths: List[OriginDestinationData])
