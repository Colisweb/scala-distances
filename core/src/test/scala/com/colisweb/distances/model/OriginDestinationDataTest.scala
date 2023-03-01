package com.colisweb.distances.model

import com.colisweb.distances.model.path.DirectedPath
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class OriginDestinationDataTest extends AnyWordSpec {

  "OriginDestinationData" should {
    "compute elevation angle" in {
      val v1         = DirectedPath(Point(1, 1, Some(1)), Point(1, 1.0001, Some(11)))
      val v1Reversed = DirectedPath(Point(1, 1.0001, Some(11)), Point(1, 1, Some(1)))

      v1.elevationAngleInDegrees shouldBe 41.97 +- 0.01
      v1Reversed.elevationAngleInDegrees shouldBe -41.97 +- 0.01

      val v2 = DirectedPath(Point(1, 2, Some(3)), Point(1, 2, Some(3)))
      v2.elevationAngleInDegrees shouldBe 0d

      val v3 = DirectedPath(Point(2, 2, Some(3)), Point(1, 2, Some(3)))
      v3.elevationAngleInDegrees shouldBe 0d
    }
  }
}
