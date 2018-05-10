package com.guizmaii.distances

import cats.effect.IO
import com.guizmaii.distances.Types.TravelMode.Driving
import com.guizmaii.distances.Types._
import com.guizmaii.distances.utils.GoogleGeoApiContext
import monix.eval.Task
import org.scalatest.{Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

object GoogleDistanceProviderSpec extends WordSpec with Matchers {

  def passTests(
      geocode: PostalCode => LatLong,
      computeDistances: List[DirectedPath] => Map[(TravelMode, LatLong, LatLong), Distance]
  ): Unit = {

    "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      val paris01 = geocode(PostalCode("75001"))
      val paris02 = geocode(PostalCode("75002"))
      val paris18 = geocode(PostalCode("75018"))

      paris01 shouldBe LatLong(48.8640493, 2.3310526)
      paris02 shouldBe LatLong(48.8675641, 2.34399)
      paris18 shouldBe LatLong(48.891305, 2.3529867)

      val driveFrom01to02 = DirectedPath(origin = paris01, destination = paris02, Driving :: Nil)
      val driveFrom01to18 = DirectedPath(origin = paris01, destination = paris18, Driving :: Nil)

      val results = computeDistances(driveFrom01to02 :: driveFrom01to18 :: Nil)

      results shouldBe Map(
        (Driving, paris01, paris02) -> Distance(1670.0 meters, 516 seconds),
        (Driving, paris01, paris18) -> Distance(5474.0 meters, 1445 seconds)
      )

      results((Driving, paris01, paris02)).length should be < results((Driving, paris01, paris18)).length
      results((Driving, paris01, paris02)).duration should be < results((Driving, paris01, paris18)).duration
    }
  }

}

class GoogleDistanceProviderSpec extends WordSpec with Matchers {

  import GoogleDistanceProviderSpec._

  lazy val geoContext: GoogleGeoApiContext = {
    val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
    GoogleGeoApiContext(googleApiKey)
  }

  "GoogleDistanceApi.distance" should {
    "pass tests with cats-effect IO" should {
      val geocoder: GeoProvider[IO]         = GoogleGeoProvider(geoContext)
      val distanceApi: DistanceProvider[IO] = GoogleDistanceProvider(geoContext)

      passTests(
        postalCode => geocoder.geocode(postalCode).unsafeRunSync(),
        paths => distanceApi.distances(paths).unsafeRunSync()
      )
    }
    "pass tests with Monix Task" should {
      import monix.execution.Scheduler.Implicits.global

      val geocoder: GeoProvider[Task]         = GoogleGeoProvider(geoContext)
      val distanceApi: DistanceProvider[Task] = GoogleDistanceProvider(geoContext)

      passTests(
        postalCode => geocoder.geocode(postalCode).runSyncUnsafe(10 seconds),
        paths => distanceApi.distances(paths).runSyncUnsafe(10 seconds)
      )
    }
  }

}
