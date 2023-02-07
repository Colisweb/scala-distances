package com.colisweb.distances.util

import cats.MonadError
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{DistanceError, PathResult}
import com.colisweb.distances.{DistanceApi, Distances}

class FromMapDistances[F[_]](implicit F: MonadError[F, Throwable]) {

  def fromMapOrError(
      data: Map[DirectedPath, PathResult],
      error: => DistanceError
  ): Distances[F, DirectedPath] =
    Distances(new FromMapDistanceApi(data, error))

  def fromMap(data: Map[DirectedPath, PathResult]): Distances[F, DirectedPath] =
    fromMapOrError(data, TestTypes.Error)

  def empty: Distances[F, DirectedPath] =
    fromMap(Map.empty[DirectedPath, PathResult])

  def emptyAndError(error: => DistanceError): Distances[F, DirectedPath] =
    fromMapOrError(Map.empty[DirectedPath, PathResult], error)

}

class FromMapDistanceApi[F[_]](data: Map[DirectedPath, PathResult], error: => DistanceError)(implicit
    F: MonadError[F, Throwable]
) extends DistanceApi[F, DirectedPath] {

  override def distance(path: DirectedPath, segments: Int = 1): F[PathResult] =
    data.get(path) match {
      case Some(pathResult) => F.pure(pathResult)
      case None             => F.raiseError(error)
    }
}

object FromMapDistances {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): FromMapDistances[F] = new FromMapDistances[F]
}
