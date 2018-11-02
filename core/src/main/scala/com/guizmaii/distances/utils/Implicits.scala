package com.guizmaii.distances.utils

import cats.kernel.Semigroup

import scala.collection.generic.CanBuildFrom
import scala.collection.{TraversableLike, mutable}

private[distances] object Implicits {

  implicit final class TraversableLikeOps[A, Repr](val coll: TraversableLike[A, Repr]) extends AnyVal {

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
      import cats.syntax.monoid._

      val acc: mutable.Map[B, A] = mutable.Map()

      for (a <- coll) {
        val k: B = key(a)
        acc += k -> acc.get(k).fold(a)(_ |+| a)
      }

      val builder = bf(coll.repr)
      for (elem <- acc) { builder += elem._2 }
      builder.result()
    }
  }

}
