package com.colisweb.distances

import com.colisweb.distances.model.PathResult

trait DistanceApi[F[_], P] {

  // todo: remove segments + segment approximation, Malt will do that

  // todo: pour malt, vérfier si on peut pas parcourir les segments qu'une seule fois dans le précalcul et en tirer un coefficient qui sera multiplé au poids
  def distance(path: P): F[PathResult]
}
