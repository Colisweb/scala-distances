package com.colisweb.distances.model

import com.colisweb.distances.model.path.DirectedPath

final case class PathResult(distance: DistanceInKm, duration: DurationInSeconds, paths: List[DirectedPath])
