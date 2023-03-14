package com.colisweb.distances.providers.here

import com.colisweb.distances.bird.Haversine
import com.colisweb.distances.model.Point
import com.colisweb.distances.model.path.DirectedPath
import com.github.writethemfirst.Approbation
import com.github.writethemfirst.approvals.utils.FunctionUtils
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.Try

class HereRoutingProviderTest extends FixtureAnyFlatSpec with Matchers with Approbation {

  "HereRoutingProviderTest.computeElevationProfile" should "be compliant" in { approver =>
    val hereProvider = new HereRoutingProvider[Try](null, null)(RoutingMode.MinimalDistanceMode)

    val latOffsetFor10Km = 0.09
    val origin           = Point(0, 0, Some(0))
    val speedInKmH       = 60d
    val speedInMPerS     = speedInKmH * 1000 / 3600d

    val angles: List[List[Double]] = List(
      Nil,
      List(0),
      List(0, 0),
      List(0, 0, 0),
      List(15),
      List(30),
      List(45),
      List(-15),
      List(-30),
      List(-45),
      List(15, -15),
      List(45, -45),
      List(89, -89),
      List(3, -10),
      List(30, -1),
      List(10, 45, -2),
      List(-45, 0, 1)
    )

    @tailrec
    def genSubPaths(leftAngles: List[Double], acc: List[DirectedPath]): List[DirectedPath] = {
      leftAngles match {
        case Nil => acc.reverse
        case nextAngle :: tail =>
          val currentOrigin = acc.headOption.map(_.destination).getOrElse(origin)
          val destination   = currentOrigin.copy(latitude = currentOrigin.latitude + latOffsetFor10Km)
          val distanceInKm  = Haversine.distanceInKm(currentOrigin, destination)

          val updatedDestination = destination.copy(
            elevation = Some(currentOrigin.elevation.get + distanceInKm * 1000 * math.tan(nextAngle.toRadians))
          )

          val subPath = DirectedPath(currentOrigin, updatedDestination)
          genSubPaths(tail, subPath :: acc)
      }
    }

    def compute(angles: List[Double]): Double = {
      val subPaths = genSubPaths(angles, Nil)
      val totalDistanceInKm = subPaths.map { path =>
        path.birdDistanceInKm / math.cos(path.elevationAngleInRadians)
      }.sum

      hereProvider.computeElevationProfile(
        subPaths = subPaths,
        totalDuration = (totalDistanceInKm * 1000 / speedInMPerS).round,
        totalDistance = totalDistanceInKm
      )
    }

    val result = FunctionUtils.applyCombinations(angles.asJava, compute)

    approver.verify(s"""elevation profile - angles
         |$result""".stripMargin)
  }
}
