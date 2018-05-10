package com.guizmaii.distances.implementations.google.distanceapi

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

  import monix.execution.Scheduler.Implicits.global

  lazy val geoContext: GoogleGeoApiContext = {
    val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
    GoogleGeoApiContext(googleApiKey)
  }
  lazy val geocoder: Geocoder       = GoogleGeocoder(geoContext)
  lazy val distanceApi: DistanceApi = GoogleDistanceApi(geoContext)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(500, Millis))

  "GoogleDistanceApi.distanceT" should {
    "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      implicit val cache: GeoCache[LatLong]               = InMemoryGeoCache[LatLong](1 day)
      implicit val distCache: GeoCache[CacheableDistance] = InMemoryGeoCache[CacheableDistance](1 day)

      val paris01 = geocoder.geocodePostalCodeT(PostalCode("75001")).runAsync
      val paris02 = geocoder.geocodePostalCodeT(PostalCode("75002")).runAsync
      val paris18 = geocoder.geocodePostalCodeT(PostalCode("75018")).runAsync

      whenReady(paris01.zip(paris02).zip(paris18)) {
        case ((parie01V, parie02V), parie18V) =>
          parie01V shouldBe LatLong(48.8640493, 2.3310526)
          parie02V shouldBe LatLong(48.8675641, 2.34399)
          parie18V shouldBe LatLong(48.891305, 2.3529867)

          val from01to02 = distanceApi.distanceT(parie01V, parie02V, Driving :: Nil).runAsync
          val from01to18 = distanceApi.distanceT(parie01V, parie18V, Driving :: Nil).runAsync

          whenReady(from01to02.zip(from01to18)) {
            case ((from01to02V, from01to18V)) =>
              from01to02V shouldBe Map(Driving -> Distance(1670.0 meters, 516 seconds))
              from01to18V shouldBe Map(Driving -> Distance(5474.0 meters, 1445 seconds))

              from01to02V(Driving).length should be < from01to18V(Driving).length
              from01to02V(Driving).duration should be < from01to18V(Driving).duration
          }
      }
    }
  }

}
