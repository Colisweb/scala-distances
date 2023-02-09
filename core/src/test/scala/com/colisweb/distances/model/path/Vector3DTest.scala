package com.colisweb.distances.model.path

import com.colisweb.distances.model.Point
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class Vector3DTest extends AnyWordSpec {

  "Vector3DTest" should {
    "compute elevation angle and know whether the slope is descending" in {
      val v1         = DirectedPath(Point(1, 1, Some(1)), Point(1, 2, Some(2)))
      val v1Reversed = DirectedPath(Point(1, 2, Some(2)), Point(1, 1, Some(1)))

      v1.elevationAngleInDegrees shouldBe 45d +- 0.01
      v1.descendingSlope shouldBe Some(false)
      v1Reversed.elevationAngleInDegrees shouldBe 45d +- 0.01
      v1Reversed.descendingSlope shouldBe Some(true)

      val v2         = DirectedPath(Point(1, 2, Some(3)), Point(4, 1, Some(6)))
      val v2Reversed = DirectedPath(Point(4, 1, Some(6)), Point(1, 2, Some(3)))

      v2.elevationAngleInDegrees shouldBe 43.49 +- 0.01
      v2.descendingSlope shouldBe Some(false)
      v2Reversed.elevationAngleInDegrees shouldBe 43.49 +- 0.01
      v2Reversed.descendingSlope shouldBe Some(true)
    }
  }
}
