package com.guizmaii.distances.implementations.google.distanceapi

import cats.effect.IO
import com.guizmaii.distances.Types.TravelMode.Driving
import com.guizmaii.distances.Types.{CacheableDistance, Distance, LatLong, PostalCode}
import com.guizmaii.distances.implementations.cache.InMemoryGeoCache
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.implementations.google.geocoder.GoogleGeocoder
import com.guizmaii.distances.{DistanceApi, GeoCache, Geocoder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import squants.space.LengthConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

class GoogleDistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  lazy val geoContext: GoogleGeoApiContext = {
    val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
    GoogleGeoApiContext(googleApiKey)
  }
  lazy val geocoder: Geocoder       = GoogleGeocoder(geoContext)
  lazy val distanceApi: DistanceApi = GoogleDistanceApi(geoContext)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(500, Millis))

  "GoogleDistanceApi.distance" should {
    "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      implicit val cache: GeoCache[LatLong]               = InMemoryGeoCache[LatLong](1 day)
      implicit val distCache: GeoCache[CacheableDistance] = InMemoryGeoCache[CacheableDistance](1 day)

      val paris01 = geocoder.geocodePostalCode[IO](PostalCode("75001")).unsafeRunSync()
      val paris02 = geocoder.geocodePostalCode[IO](PostalCode("75002")).unsafeRunSync()
      val paris18 = geocoder.geocodePostalCode[IO](PostalCode("75018")).unsafeRunSync()

      paris01 shouldBe LatLong(48.8640493, 2.3310526)
      paris02 shouldBe LatLong(48.8675641, 2.34399)
      paris18 shouldBe LatLong(48.891305, 2.3529867)

      val from01to02 = distanceApi.distance[IO](paris01, paris02, Driving :: Nil).unsafeRunSync()
      val from01to18 = distanceApi.distance[IO](paris01, paris18, Driving :: Nil).unsafeRunSync()

      from01to02 shouldBe Map(Driving -> Distance(1670.0 meters, 516 seconds))
      from01to18 shouldBe Map(Driving -> Distance(4747.0 meters, 1240 seconds))

      from01to02(Driving).length should be < from01to18(Driving).length
      from01to02(Driving).duration should be < from01to18(Driving).duration
    }
  }

}
