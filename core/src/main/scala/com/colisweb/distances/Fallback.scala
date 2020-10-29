package com.colisweb.distances

import cats.MonadError
import cats.implicits._
import com.colisweb.distances.model.{DistanceAndDuration, DistanceError}

case class Fallback[F[_], P](
    first: DistanceApi[F, P],
    second: DistanceApi[F, P],
    when: PartialFunction[Throwable, F[Unit]]
)(implicit F: MonadError[F, Throwable])
    extends DistanceApi[F, P] {

  override def distance(path: P): F[DistanceAndDuration] =
    first.distance(path).recoverWith(when.andThen(_ *> second.distance(path)))
}

object Fallback {

  def apply[F[_], P](first: DistanceApi[F, P], second: DistanceApi[F, P])(implicit
      F: MonadError[F, Throwable]
  ): Fallback[F, P] = {
    val when: PartialFunction[Throwable, F[Unit]] = { case _: DistanceError => F.unit }
    Fallback[F, P](first, second, when)
  }
}
