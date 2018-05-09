package com.guizmaii.distances.utils

import cats.implicits._
import cats.kernel.Semigroup
import com.google.maps.PendingResult
import com.google.maps.model.{DistanceMatrixElement, LatLng}
import com.guizmaii.distances.Types.{LatLong, SerializableDistance}
import monix.eval.Task

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.Promise

private[distances] object RichImplicits {
  implicit final class RichGoogleLatLng(val latLng: LatLng) extends AnyVal {
    def toInnerLatLong: LatLong = LatLong(latitude = latLng.lat, longitude = latLng.lng)
  }

  private[this] final class CallBack[T](promise: Promise[T]) extends PendingResult.Callback[T] {
    override def onResult(t: T): Unit = {
      val _ = promise.success(t)
    }

    override def onFailure(throwable: Throwable): Unit = {
      val _ = promise.failure(throwable)
    }
  }

  implicit final class RichPendingResult[T](val request: PendingResult[T]) extends AnyVal {
    def toTask: Task[T] = Task.deferFuture {
      val promise = Promise[T]()
      request.setCallback(new CallBack(promise))
      promise.future
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

  implicit final class RichTaskCompanion(val taskCompanion: Task.type) extends AnyVal {

    /**
      * Launch the 3 Tasks side effects in parallele but returns the result in order:
      *
      * If the first one succeed, it returns its result
      * else if the second one succeed, it returns its result
      * else if the last one succeed, it returns its result
      * else return the error of last one.
      *
      * @param ta
      * @param tb
      * @param tc
      * @tparam A
      * @return
      */
    def raceInOrder3[A](ta: Task[A], tb: Task[A], tc: Task[A]): Task[A] = {
      Task.zip3(ta.attempt, tb.attempt, tc.attempt).flatMap {
        case (Right(v), _, _)             => Task.now(v)
        case (Left(_), Right(v), _)       => Task.now(v)
        case (Left(_), Left(_), Right(v)) => Task.now(v)
        case (Left(_), Left(_), Left(e))  => Task.raiseError(e)
      }
    }
  }

}
