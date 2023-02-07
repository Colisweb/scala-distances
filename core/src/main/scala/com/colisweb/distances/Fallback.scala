package com.colisweb.distances

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.{DistanceError, PathResult}

case class Fallback[F[_], P](
    first: DistanceApi[F, P],
    second: DistanceApi[F, P],
    when: PartialFunction[Throwable, F[Unit]]
)(implicit F: MonadError[F, Throwable])
    extends DistanceApi[F, P] {

  override def distance(path: P, segments: Int = 1): F[PathResult] =
    first.distance(path, segments).recoverWith(when.andThen(_ *> second.distance(path, segments)))
}

object Fallback {

  def apply[F[_], P](first: DistanceApi[F, P], second: DistanceApi[F, P])(implicit
      F: MonadError[F, Throwable]
  ): Fallback[F, P] = {
    val when: PartialFunction[Throwable, F[Unit]] = { case _: DistanceError => F.unit }
    Fallback[F, P](first, second, when)
  }
}
