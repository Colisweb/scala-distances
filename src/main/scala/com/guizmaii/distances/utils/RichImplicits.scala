package com.guizmaii.distances.utils

import cats.effect.Async
import cats.kernel.Semigroup
import com.google.maps.PendingResult

import scala.collection.mutable.{Map => MutableMap}

private[distances] object RichImplicits {

  import cats.implicits._

  implicit final class RichPendingResult[AIO[_], T](val request: PendingResult[T])(implicit AIO: Async[AIO]) {
    def asEffect: AIO[T] =
      AIO.async { cb =>
        request.setCallback(new PendingResult.Callback[T] {
          override def onResult(result: T): Unit     = cb(Right(result))
          override def onFailure(e: Throwable): Unit = cb(Left(e))
        })
      }
  }

  implicit final class RichList[Value](val list: List[Value]) extends AnyVal {
    def combineDuplicatesOn[Key](key: Value => Key)(implicit sm: Semigroup[Value]): List[Value] = {
      val zero: MutableMap[Key, Value] = MutableMap()

      list
        .foldLeft(zero) {
          case (acc, a) =>
            val k: Key = key(a)
            acc += k -> acc.get(k).fold(a)(_ |+| a)
        }
        .values
        .toList
    }
  }

  implicit final class Tuple3AsyncOps[AIO[_], A](val instance: (AIO[A], AIO[A], AIO[A]))(implicit AIO: Async[AIO]) {
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
    def raceInOrder3(implicit par: Par[AIO]): AIO[A] = {
      val (a, b, c) = instance
      (a.attempt, b.attempt, c.attempt).parTupled
        .flatMap {
          case (Right(v), _, _)             => AIO.pure(v)
          case (Left(_), Right(v), _)       => AIO.pure(v)
          case (Left(_), Left(_), Right(v)) => AIO.pure(v)
          case (Left(_), Left(_), Left(e))  => AIO.raiseError(e)
        }
    }
  }

}
