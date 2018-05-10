package com.guizmaii.distances.implementations.google.distanceapi

import cats.effect.{Async, IO}
import com.guizmaii.distances.Types.TravelMode.Driving
import com.guizmaii.distances.Types.{Distance, LatLong, PostalCode, TravelMode}
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.implementations.google.geocoder.GoogleGeocoder
import com.guizmaii.distances.{DistanceApi, Geocoder}
import monix.eval.Task
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

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(500, Millis))

  def passTests[AIO[+ _]: Async](runSync: AIO[Any] => Any): Unit = {

    val geocoder: Geocoder[AIO]       = GoogleGeocoder(geoContext)
    val distanceApi: DistanceApi[AIO] = GoogleDistanceApi(geoContext)

    "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
      val paris01 = runSync(geocoder.geocodePostalCode(PostalCode("75001"))).asInstanceOf[LatLong]
      val paris02 = runSync(geocoder.geocodePostalCode(PostalCode("75002"))).asInstanceOf[LatLong]
      val paris18 = runSync(geocoder.geocodePostalCode(PostalCode("75018"))).asInstanceOf[LatLong]

      paris01 shouldBe LatLong(48.8640493, 2.3310526)
      paris02 shouldBe LatLong(48.8675641, 2.34399)
      paris18 shouldBe LatLong(48.891305, 2.3529867)

      val from01to02 = runSync(distanceApi.distance(paris01, paris02, Driving :: Nil)).asInstanceOf[Map[TravelMode, Distance]]
      val from01to18 = runSync(distanceApi.distance(paris01, paris18, Driving :: Nil)).asInstanceOf[Map[TravelMode, Distance]]

      from01to02 shouldBe Map(Driving -> Distance(1670.0 meters, 516 seconds))
      from01to18 shouldBe Map(Driving -> Distance(5474.0 meters, 1445 seconds))

      from01to02(Driving).length should be < from01to18(Driving).length
      from01to02(Driving).duration should be < from01to18(Driving).duration
    }
  }

  "GoogleDistanceApi.distance" should {
    "with cats-effect IO" should {
      passTests[IO](_.unsafeRunSync())
    }
    "with Monix Task" should {
      import monix.execution.Scheduler.Implicits.global

      passTests[Task](_.runSyncUnsafe(10 seconds))
    }
  }

}
