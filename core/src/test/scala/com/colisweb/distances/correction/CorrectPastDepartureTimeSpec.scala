package com.colisweb.distances.correction

import cats.Id
import com.colisweb.distances.model.{PathResult, TravelMode}
import com.colisweb.distances.model.path.DirectedPathWithModeAt
import com.colisweb.distances.util.TestValues._
import com.colisweb.distances.{DistanceApi, Distances}
import org.mockito.scalatest.IdiomaticMockito
import org.mockito.{ArgumentMatchersSugar, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}
import scala.concurrent.duration._

class CorrectPastDepartureTimeSpec
    extends AnyWordSpec
    with Matchers
    with IdiomaticMockito
    with ArgumentMatchersSugar
    with BeforeAndAfterEach {

  private val now   = Instant.parse("2021-10-01T12:00:00Z")
  private val clock = Clock.fixed(now, UTC)
  private val base  = mock[DistanceApi[Id, DirectedPathWithModeAt]]

  private val margin: FiniteDuration = 1.hour
  private val api                    = Distances.from(base).correctPastDepartureTime(margin, clock).api

  private def path(departureTime: Option[Instant]): DirectedPathWithModeAt =
    DirectedPathWithModeAt(p1, p2, TravelMode.Car(), departureTime)

  override protected def beforeEach(): Unit = {
    Mockito.reset(base)
    base.distance(any[DirectedPathWithModeAt]) returns PathResult(0d, 0, Nil)
    ()
  }

  "DistanceApi with correctPastDepartureTime" should {

    "update a departureTime widely in the past" in {
      val departureTime = now.minus(3, ChronoUnit.HOURS)
      api.distance(path(Some(departureTime)))

      val expectedCorrection = Some(now.plusSeconds(margin.toSeconds))
      base.distance(path(expectedCorrection)) wasCalled once
    }

    "update a departureTime considered in the past within margin" in {
      val departureTime = now.plus(30, ChronoUnit.MINUTES)
      api.distance(path(Some(departureTime)))

      val expectedCorrection = Some(now.plusSeconds(margin.toSeconds))
      base.distance(path(expectedCorrection)) wasCalled once
    }

    "do nothing for a departureTime in the future" in {
      val departureTime = now.plus(3, ChronoUnit.HOURS)
      api.distance(path(Some(departureTime)))

      base.distance(path(Some(departureTime))) wasCalled once
    }

    "do nothing when no departureTime is given" in {
      api.distance(path(None))
      base.distance(path(None)) wasCalled once
    }
  }
}
