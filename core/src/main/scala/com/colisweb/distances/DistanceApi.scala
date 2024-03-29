package com.colisweb.distances

import com.colisweb.distances.model.PathResult

trait DistanceApi[F[_], P] {

  def distance(path: P): F[PathResult]
}
