package com.colisweb.distances.cache

import com.colisweb.distances.Distances
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{PathResult, Point}
import com.colisweb.distances.util.FromMapDistances
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Try}

class CacheSpec extends AnyWordSpec with Matchers {

  private val failingCache: Cache[Try, DirectedPath, PathResult] =
    new Cache[Try, DirectedPath, PathResult] {
      private val failure: Failure[Nothing]                            = Failure(new RuntimeException("failure"))
      override def get(key: DirectedPath): Try[Option[PathResult]]     = failure
      override def put(key: DirectedPath, value: PathResult): Try[Any] = failure
    }

  private val path: DirectedPath     = DirectedPath(Point(0, 0), Point(0, 0))
  private val pathResult: PathResult = PathResult(1, 1)

  private val distanceMap: Distances[Try, DirectedPath] =
    FromMapDistances[Try]
      .fromMap(Map(path -> pathResult))
      .caching(failingCache)

  "Failing cache" should {
    "not propagate failure on caching" in {
      distanceMap.api.distance(path).get shouldBe pathResult
    }
  }
}
