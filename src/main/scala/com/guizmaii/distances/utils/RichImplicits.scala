package com.guizmaii.distances.utils

import cats.effect.Async
import cats.kernel.Semigroup
import com.google.maps.PendingResult

import scala.collection.generic.CanBuildFrom
import scala.collection.{TraversableLike, mutable}

private[distances] object RichImplicits {

  implicit final class RichPendingResult[AIO[_], T](val request: PendingResult[T])(implicit AIO: Async[AIO]) {
    def asEffect: AIO[T] =
      AIO.async { cb =>
        request.setCallback(new PendingResult.Callback[T] {
          override def onResult(result: T): Unit     = cb(Right(result))
          override def onFailure(e: Throwable): Unit = cb(Left(e))
        })
      }
  }

  implicit final class TraversableLikeOps[A, Repr](val coll: TraversableLike[A, Repr]) extends AnyVal {

    // TODO: Is it possible to do that with only one pass ? Maybe by manually manipulating an index.
    /**
      * Inspired by: https://github.com/cvogt/scala-extensions/blob/master/src/main/scala/collection.scala#L14-L28
      *
      * For more information, see: https://scala-lang.org/blog/2017/05/30/tribulations-canbuildfrom.html
      *
      * @param key
      * @param bf
      * @tparam B
      * @tparam That
      * @return
      */
    def combineDuplicatesOn[B, That](key: A => B)(implicit A: Semigroup[A], bf: CanBuildFrom[Repr, A, That]): That = {
      val acc: mutable.Map[B, A] = mutable.Map()

      for (a <- coll) {
        val k: B = key(a)
        acc += k -> acc.get(k).fold(a)(A.combine(_, a))
      }

      val builder = bf(coll.repr)
      for (elem <- acc) { builder += elem._2 }
      builder.result()
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
