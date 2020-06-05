package com.colisweb.distances.utils

import cats.kernel.Semigroup

import scala.collection.mutable

private[distances] object Implicits {

  implicit final class TraversableLikeOps[A](val coll: List[A]) extends AnyVal {

    /**
      * Inspired by: https://github.com/cvogt/scala-extensions/blob/master/src/main/scala/collection.scala#L14-L28
      *
      * For more information, see: https://scala-lang.org/blog/2017/05/30/tribulations-canbuildfrom.html
      *
      * @param key
      * @tparam B
      * @return
      */
    def combineDuplicatesOn[B](
        key: A => B
    )(implicit A: Semigroup[A]): List[A] = {
      import cats.syntax.semigroup._

      val acc: mutable.Map[B, A] = mutable.Map()

      for (a <- coll) {
        val k: B = key(a)
        acc += k -> acc.get(k).fold(a)(_ |+| a)
      }

      acc.values.toList
    }
  }

}
