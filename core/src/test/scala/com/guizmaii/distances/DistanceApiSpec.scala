package com.guizmaii.distances

import cats.effect.IO
import com.guizmaii.distances.Types.TravelMode.{Bicycling, Driving}
import com.guizmaii.distances.Types.{Distance, LatLong, PostalCode}
import com.guizmaii.distances.utils.GoogleGeoApiContext
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import com.guizmaii.distances.utils.Stubs._

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
      "pass the same test suite than GoogleDistanceProvider" in {
        lazy val geoContext: GoogleGeoApiContext = {
          val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
          GoogleGeoApiContext(googleApiKey)
        }

        "pass tests with cats-effect IO" should {
          val geocoder: GeoProvider[IO]    = GoogleGeoProvider(geoContext)
          val distanceApi: DistanceApi[IO] = DistanceApi(GoogleDistanceProvider(geoContext))

          GoogleDistanceProviderSpec.passTests(
            postalCode => geocoder.geocode(postalCode).unsafeRunSync(),
            paths => distanceApi.distances(paths).unsafeRunSync()
          )
        }
        "pass tests with Monix Task" should {
          import monix.execution.Scheduler.Implicits.global

          val geocoder: GeoProvider[Task]    = GoogleGeoProvider(geoContext)
          val distanceApi: DistanceApi[Task] = DistanceApi(GoogleDistanceProvider(geoContext))

          GoogleDistanceProviderSpec.passTests(
            postalCode => geocoder.geocode(postalCode).runSyncUnsafe(10 seconds),
            paths => distanceApi.distances(paths).runSyncUnsafe(10 seconds)
          )
        }
      }
    }

  }

}
