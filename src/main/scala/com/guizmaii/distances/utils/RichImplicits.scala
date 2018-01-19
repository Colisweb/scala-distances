package com.guizmaii.distances.utils

import com.google.maps.PendingResult
import com.google.maps.model.{DistanceMatrixElement, LatLng}
import com.guizmaii.distances.Types.{LatLong, SerializableDistance}
import monix.eval.Task

import scala.collection.{IterableLike, TraversableLike}
import scala.collection.generic.CanBuildFrom
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

  implicit final class RichCollection[A, Repr](val xs: IterableLike[A, Repr]) extends AnyVal {

    /**
      * Come from here: https://stackoverflow.com/a/34456552/2431728
      *
      * @param f
      * @param cbf
      * @tparam B
      * @return
      */
    def distinctBy[B, That](f: A => B)(implicit cbf: CanBuildFrom[Repr, A, That]): That = {
      val builder = cbf(xs.repr)
      val i       = xs.toIterator
      var set     = Set[B]()
      while (i.hasNext) {
        val o = i.next
        val b = f(o)
        if (!set(b)) {
          set += b
          builder += o
        }
      }
      builder.result
    }
  }

}
