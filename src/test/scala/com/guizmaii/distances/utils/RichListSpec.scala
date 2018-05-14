package com.guizmaii.distances.utils

import cats.kernel.Semigroup
import com.guizmaii.distances.DistanceApi
import com.guizmaii.distances.Types.TravelMode._
import com.guizmaii.distances.Types.{DirectedPath, LatLong}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

class RichListSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach with PrivateMethodTester {

  "RichList#combineDuplicatesOn" should {

    import RichImplicits._

    val directedPathSemiGroup: PrivateMethod[Semigroup[DirectedPath]] = PrivateMethod[Semigroup[DirectedPath]]('directedPathSemiGroup)

    "remove duplicates and combine using the semigroup" in {
      val a00 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a01 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a11 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Bicycling :: Nil)
      val a21 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a22 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a23 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)
      val a24 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Unknown :: Nil)

      val res = List(a00, a01, a11, a21, a22, a23, a24).combineDuplicatesOn { case DirectedPath(o, d, _) => (o, d) }(
        DistanceApi invokePrivate directedPathSemiGroup())

      res.head shouldBe DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Bicycling :: Unknown :: Nil)
    }
    "remove duplicates and combine using the semigroup, BIS" in {
      val a00 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a01 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Nil)
      val a11 = DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Bicycling :: Nil)
      val b00 = DirectedPath(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Nil)
      val b01 = DirectedPath(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Nil)
      val b02 = DirectedPath(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Unknown :: Nil)
      val res = List(a00, a01, a11, b00, b01, b02).combineDuplicatesOn { case DirectedPath(o, d, _) => (o, d) }(
        DistanceApi invokePrivate directedPathSemiGroup())

      res shouldBe Vector(
        DirectedPath(LatLong(1.0, 1.0), LatLong(2.0, 2.0), Driving :: Unknown :: Nil),
        DirectedPath(LatLong(42.0, 42.0), LatLong(43.0, 43.0), Driving :: Bicycling :: Nil)
      )
    }
  }

}
