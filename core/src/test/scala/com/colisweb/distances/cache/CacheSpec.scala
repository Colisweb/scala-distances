package com.colisweb.distances.cache

import cats.effect.IO
import com.colisweb.distances.Distances
import com.colisweb.distances.model.path.DirectedPath
import com.colisweb.distances.model.{PathResult, Point}
import com.colisweb.distances.util.FromMapDistances
import com.colisweb.simplecache.memory.MemoryCache
import com.colisweb.simplecache.wrapper.cats.CatsCache
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CacheSpec extends AnyWordSpec with Matchers {

  private val failingCache: CatsCache[IO, DirectedPath, PathResult] = {
    new CatsCache[IO, DirectedPath, PathResult](new MemoryCache()) {
      private val failure: IO[Nothing]                            = IO.raiseError(new RuntimeException("failure"))
      override def get(key: DirectedPath): IO[Option[PathResult]] = failure
      override def update(key: DirectedPath, value: PathResult): IO[Unit] = failure
    }
  }

  private val path: DirectedPath     = DirectedPath(Point(0, 0), Point(0, 0))
  private val pathResult: PathResult = PathResult(1, 1)

  private val distanceMap: Distances[IO, DirectedPath] =
    FromMapDistances[IO]
      .fromMap(Map(path -> pathResult))
      .caching(failingCache)

  "Failing cache" should {
    "not propagate failure on caching" in {
      distanceMap.api.distance(path).unsafeRunSync() shouldBe pathResult
    }
  }
}
