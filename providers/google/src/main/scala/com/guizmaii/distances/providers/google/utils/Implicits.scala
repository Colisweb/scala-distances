package com.guizmaii.distances.providers.google.utils

import cats.MonadError
import cats.effect.Concurrent
import com.google.maps.PendingResult

private[google] object Implicits {

  implicit final class PendingResultOps[T](val request: PendingResult[T]) {
    def asEffect[F[_]: Concurrent]: F[T] =
      Concurrent[F].cancelable { cb =>
        request.setCallback(new PendingResult.Callback[T] {
          override def onResult(result: T): Unit     = cb(Right(result))
          override def onFailure(e: Throwable): Unit = cb(Left(e))
        })

        Concurrent[F].delay(request.cancel())
      }
  }

  implicit final class Tuple3AsyncOps[F[_], A](val instance: (F[A], F[A], F[A])) {
    import cats.implicits._
    import cats.temp.par._

    /**
      * Launch the 3 Tasks side effects in parallele but returns the result in order:
      *
      * If the first one succeed, it returns its result
      * else if the second one succeed, it returns its result
      * else if the last one succeed, it returns its result
      * else return the error of last one.
      *
      */
    def raceInOrder3(implicit F: MonadError[F, Throwable], par: Par[F]): F[A] = {
      val (a, b, c) = instance
      (a.attempt, b.attempt, c.attempt).parTupled
        .flatMap {
          case (Right(v), _, _)             => F.pure(v)
          case (Left(_), Right(v), _)       => F.pure(v)
          case (Left(_), Left(_), Right(v)) => F.pure(v)
          case (Left(_), Left(_), Left(e))  => F.raiseError(e)
        }
    }
  }

}
