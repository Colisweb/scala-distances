package com.guizmaii.distances.utils

import cats.effect.Effect
import cats.kernel.Semigroup
import com.google.maps.PendingResult
import com.google.maps.model.{DistanceMatrixElement, LatLng}
import com.guizmaii.distances.Types.{LatLong, SerializableDistance}

import scala.collection.mutable.{Map => MutableMap}

private[distances] object RichImplicits {

  import cats.implicits._

  implicit final class RichGoogleLatLng(val latLng: LatLng) extends AnyVal {
    def toInnerLatLong: LatLong = LatLong(latitude = latLng.lat, longitude = latLng.lng)
  }

  implicit final class RichPendingResult[E[_], T](val request: PendingResult[T])(implicit E: Effect[E]) {
    def asEffect: E[T] =
      E.async { cb =>
        request.setCallback(new PendingResult.Callback[T] {
          override def onResult(result: T): Unit     = cb(Right(result))
          override def onFailure(e: Throwable): Unit = cb(Left(e))
        })
      }
  }

  implicit final class RichDistanceMatrixElement(val element: DistanceMatrixElement) extends AnyVal {
    def asSerializableDistance: SerializableDistance =
      SerializableDistance(value = element.distance.inMeters.toDouble, duration = element.duration.inSeconds.toDouble)
  }

  implicit final class RichList[Value](val list: List[Value]) extends AnyVal {
    def combineDuplicatesOn[Key](key: Value => Key)(implicit sm: Semigroup[Value]): Vector[Value] = {
      val zero: MutableMap[Key, Value] = MutableMap()

      list
        .foldLeft(zero) {
          case (acc, a) =>
            val k: Key = key(a)
            acc += k -> acc.get(k).fold(a)(_ |+| a)
        }
        .values
        .toVector
    }
  }

  implicit final class RichEffectCompanion(val taskCompanion: Effect.type) {

    import cats.implicits._

    /**
      * Launch the 3 Tasks side effects in parallele but returns the result in order:
      *
      * If the first one succeed, it returns its result
      * else if the second one succeed, it returns its result
      * else if the last one succeed, it returns its result
      * else return the error of last one.
      *
      * @param ea
      * @param eb
      * @param ec
      * @tparam A
      * @return
      */
    def raceInOrder3[E[_], A](ea: E[A], eb: E[A], ec: E[A])(implicit E: Effect[E]): E[A] = {
      (ea.attempt, eb.attempt, ec.attempt)
        .mapN((_, _, _)) // TODO: Possible to use `parMapN` ??
        .flatMap {
          case (Right(v), _, _)             => E.pure(v)
          case (Left(_), Right(v), _)       => E.pure(v)
          case (Left(_), Left(_), Right(v)) => E.pure(v)
          case (Left(_), Left(_), Left(e))  => E.raiseError(e)
        }
    }
  }

}
