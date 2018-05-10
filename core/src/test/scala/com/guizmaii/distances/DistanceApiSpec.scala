package com.guizmaii.distances

import cats.effect.{Async, IO}
import cats.temp.par.Par
import com.guizmaii.distances.Types.TravelMode.{Bicycling, Driving}
import com.guizmaii.distances.Types._
import com.guizmaii.distances.utils.GoogleGeoApiContext
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import com.guizmaii.distances.utils.Stubs._

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"))

  "DistanceApi" should {
    "#distance" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val distanceApi: DistanceApi[IO] = DistanceApi(distanceProviderStub)
          val latLong                      = LatLong(0.0, 0.0)
          val expectedResult               = Map((Driving, Distance.zero), (Bicycling, Distance.zero))
          distanceApi.distance(latLong, latLong, Driving :: Bicycling :: Nil).unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distanceFromPostalCodes" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val distanceApi: DistanceApi[IO] = DistanceApi(distanceProviderStub)
          val postalCode                   = PostalCode("59000")
          val expectedResult               = Map((Driving, Distance.zero), (Bicycling, Distance.zero))
          distanceApi
            .distanceFromPostalCodes(geocoderStub)(postalCode, postalCode, Driving :: Bicycling :: Nil)
            .unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distances" should {
      "pass the same test suite than GoogleDistanceProvider" should {
        def passTests[AIO[+ _]: Async: Par](runSync: AIO[Any] => Any): Unit = {

          val geocoder: GeoProvider[AIO]    = GoogleGeoProvider[AIO](geoContext)
          val distanceApi: DistanceApi[AIO] = DistanceApi[AIO](GoogleDistanceProvider[AIO](geoContext))

          s"says that Paris 02 is nearest to Paris 01 than Paris 18" in {
            val paris01 = runSync(geocoder.geocode(PostalCode("75001"))).asInstanceOf[LatLong]
            val paris02 = runSync(geocoder.geocode(PostalCode("75002"))).asInstanceOf[LatLong]
            val paris18 = runSync(geocoder.geocode(PostalCode("75018"))).asInstanceOf[LatLong]

            paris01 shouldBe LatLong(48.8640493, 2.3310526)
            paris02 shouldBe LatLong(48.8675641, 2.34399)
            paris18 shouldBe LatLong(48.891305, 2.3529867)

            val driveFrom01to02 = DirectedPath(origin = paris01, destination = paris02, Driving :: Nil)
            val driveFrom01to18 = DirectedPath(origin = paris01, destination = paris18, Driving :: Nil)

            val results = runSync(distanceApi.distances(driveFrom01to02 :: driveFrom01to18 :: Nil))
              .asInstanceOf[Map[(TravelMode, LatLong, LatLong), Distance]]

            results shouldBe Map(
              (Driving, paris01, paris02) -> Distance(1670.0 meters, 516 seconds),
              (Driving, paris01, paris18) -> Distance(5474.0 meters, 1445 seconds)
            )

            results((Driving, paris01, paris02)).length should be < results((Driving, paris01, paris18)).length
            results((Driving, paris01, paris02)).duration should be < results((Driving, paris01, paris18)).duration
          }
        }

        lazy val geoContext: GoogleGeoApiContext = {
          val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
          GoogleGeoApiContext(googleApiKey)
        }

        "pass tests with cats-effect IO" should {
          passTests[IO](_.unsafeRunSync())
        }
        "pass tests with Monix Task" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](_.runSyncUnsafe(10 seconds))
        }
      }
    }

  }

}
