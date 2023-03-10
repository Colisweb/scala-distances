package com.colisweb.distances.providers.here

import com.colisweb.distances.model.Point
import com.colisweb.distances.model.path.DirectedPath
import com.github.writethemfirst.Approbation
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec
import scala.util.{Random, Try}

class HereRoutingProviderTest extends FixtureAnyFlatSpec with Matchers with Approbation {

  "HereRoutingProviderTest.computeElevationProfile" should "be compliant" in { approver =>
    val hereProvider = new HereRoutingProvider[Try](null, null)(RoutingMode.MinimalDistanceMode)

    val latOffsetFor10Km = 0.09
    val origin           = Point(0, 0, Some(0))
    val distanceInKm     = 10d
    val timeInSeconds    = 15 * 60L

    val r = new Random(123L)

    val angles: List[Double] = List(0, 3, 5, 10, 15, 30, 45)
    val allAngles            = (angles ++ angles.map(-_)).map(math.toRadians)
    val numberOfSubPaths     = List(1, 2, 3)

    @tailrec
    def genSubPaths(left: Int, acc: List[DirectedPath]): List[DirectedPath] =
      if (left <= 0)
        acc.reverse
      else {
        val randomAngle        = allAngles(r.nextInt(allAngles.size - 1))
        val currentDestination = acc.headOption.map(_.destination).getOrElse(origin)

        val updatedDestination = currentDestination
          .copy(
            latitude = currentDestination.latitude + latOffsetFor10Km,
            elevation = Some(distanceInKm * 1000 * math.tan(randomAngle))
          )

        val subPath = DirectedPath(currentDestination, updatedDestination)
        genSubPaths(left - 1, subPath :: acc)
      }

    def compute(numberOfSubPaths: Int): (List[DirectedPath], Double) = {
      val subPaths = genSubPaths(numberOfSubPaths, Nil)
      subPaths -> hereProvider.computeElevationProfile(subPaths, timeInSeconds, distanceInKm)
    }

    val res: String = (0 until 10).toList
      .map { _ =>
        numberOfSubPaths
          .map { n =>
            val (subPaths, profile) = compute(n)
            s"""${subPaths.map(_.elevationAngleInDegrees.round).mkString(", ")} --- $profile"""
          }
          .mkString("\n")
      }
      .mkString("\n")

    approver.verify(s"""angles - elevation profile
         |$res""".stripMargin)
  }
}
