package com.colisweb.distances.cache

import com.colisweb.distances.Distances
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{DistanceAndDuration, Point}
import com.colisweb.distances.util.FromMapDistances
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Try}

class CacheSpec extends AnyWordSpec with Matchers {

  private val failingCache: Cache[Try, DirectedPath, DistanceAndDuration] =
    new Cache[Try, DirectedPath, DistanceAndDuration] {
      private val failure: Failure[Nothing]                                     = Failure(new RuntimeException("failure"))
      override def get(key: DirectedPath): Try[Option[DistanceAndDuration]]     = failure
      override def put(key: DirectedPath, value: DistanceAndDuration): Try[Any] = failure
    }

  private val path: DirectedPath                       = DirectedPath(Point(0, 0), Point(0, 0))
  private val distanceAndDuration: DistanceAndDuration = DistanceAndDuration(1, 1)

  private val distanceMap: Distances[Try, DirectedPath] =
    FromMapDistances[Try]
      .fromMap(Map(path -> distanceAndDuration))
      .caching(failingCache)

  "Failing cache" should {
    "not propagate failure on caching" in {
      distanceMap.api.distance(path).get shouldBe distanceAndDuration
    }
  }
}
