package com.guizmaii.distances.implementations.google.geocoder

import com.guizmaii.distances.Types.{LatLong, PostalCode}
import com.guizmaii.distances.implementations.cache.{GeoCache, InMemoryGeoCache, RedisGeoCache}
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scalacache.Id

class GoogleGeocoderSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import monix.execution.Scheduler.Implicits.global

  import scalacache.modes.sync._

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(500, Millis))

  implicit final class RichGeoCache[E <: Serializable: ClassTag](val geoCache: GeoCache[E]) {
    def get(keyParts: Any*): Id[Option[E]]     = geoCache.innerCache.get(keyParts)
    def set(keyParts: Any*)(value: E): Id[Any] = geoCache.innerCache.put(keyParts)(value)
    def flushAll(): Id[Any]                    = geoCache.innerCache.removeAll()
  }

  val lille      = LatLong(latitude = 50.6138111, longitude = 3.0423599)
  val lambersart = LatLong(latitude = 50.65583909999999, longitude = 3.0226977)
  val harnes     = LatLong(latitude = 50.4515282, longitude = 2.9047234)

  /*
  Remarque Jules:
  --------------
    Les tests sont fait sur le code postal 59000.

    Pour obtenir des données à valider, effectuez la requête suivante:
      $ curl https://maps.googleapis.com/maps/api/geocode/json?components=postal_code:59000&region=eu&key=YOUR_API_KEY
   */
  "GoogleGeocoder.geocode" should {
    "With Redis" should {
      tests(RedisGeoCache[LatLong]("localhost", 6379, 1 day))
    }
    "With 'in memory' cache" should {
      tests(InMemoryGeoCache[LatLong](1 day))
    }
  }

  def tests(cache: GeoCache[LatLong]): Unit = {
    "cache" should {
      "cache things" in {
        val c     = cache.innerCache
        val key   = "toto"
        val value = LatLong(latitude = 12, longitude = 13)
        c.put(key)(value, Some(1 day))
        c.get(key) shouldBe Some(value)
      }
    }

    "if NOT ALREADY in cache" should {
      // TODO Jules: Config CI and its Google API key
      val geoApiContext: GoogleGeoApiContext = GoogleGeoApiContext("TODO")
      val geocoder                           = new GoogleGeocoder(geoApiContext, alternativeCache = Some(cache))

      def testGeocoder(postalCode: PostalCode, place: LatLong): Assertion = {
        cache.flushAll()
        cache.get(postalCode) shouldBe None
        whenReady(geocoder.geocodeT(postalCode).runAsync) { result =>
          result shouldBe place
          cache.get(postalCode) shouldBe Some(place)
        }
      }
      "cache and return" should {
        "Lille" in {
          testGeocoder(PostalCode("59000"), lille)
        }
        "Lambersart" in {
          testGeocoder(PostalCode("59130"), lambersart)
        }
        "Harnes" in {
          testGeocoder(PostalCode("62440"), harnes)
        }
      }
    }
    "if ALREADY in cache" should {
      val geoApiContext: GoogleGeoApiContext = GoogleGeoApiContext("WRONG KEY")
      val geocoder                           = new GoogleGeocoder(geoApiContext, alternativeCache = Some(cache))

      def testGeocoder(postalCode: PostalCode, place: LatLong): Assertion = {
        cache.flushAll()
        cache.set(postalCode)(place)
        cache.get(postalCode) shouldBe Some(place)
        geocoder.geocodeT(postalCode).runAsync.futureValue shouldBe place
      }
      "just return" should {
        "Lille" in {
          testGeocoder(PostalCode("59000"), lille)
        }
        "Lambersart" in {
          testGeocoder(PostalCode("59130"), lambersart)
        }
        "Harnes" in {
          testGeocoder(PostalCode("62440"), harnes)
        }
      }
    }
  }

}
