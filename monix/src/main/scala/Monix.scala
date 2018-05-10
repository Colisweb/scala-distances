import cats.effect.Effect

import monix.eval._


object Monix {

  object Implicits {

    implicit def effects[A]: Effect[A] =

  }

}
