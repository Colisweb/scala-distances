package com.guizmaii.distances.implementations.google.geocoder

import com.guizmaii.distances.Types.{Address, LatLong, PostalCode}
import com.guizmaii.distances.implementations.cache.{InMemoryGeoCache, RedisGeoCache}
import com.guizmaii.distances.implementations.google.GoogleGeoApiContext
import com.guizmaii.distances.{GeoCache, Geocoder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest._
import shapeless.CNil

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scalacache.Id

object GoogleGeocoderSpec {
  import scalacache.modes.sync._

  implicit final class RichGeoCache[E <: Serializable: ClassTag](val cache: GeoCache[E]) {
    def get(keyParts: Any*): Id[Option[E]]     = cache.innerCache.get(keyParts)
    def set(keyParts: Any*)(value: E): Id[Any] = cache.innerCache.put(keyParts)(value)
    def flushAll(): Id[Any]                    = cache.innerCache.removeAll()
  }
}

class GoogleGeocoderSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import GoogleGeocoderSpec._
  import monix.execution.Scheduler.Implicits.global

  import scalacache.modes.sync._

  lazy val geoContext: GoogleGeoApiContext = {
    val googleApiKey: String = System.getenv().get("GOOGLE_API_KEY")
    GoogleGeoApiContext(googleApiKey)
  }
  lazy val geocoder: Geocoder = GoogleGeocoder(geoContext)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val lille                = LatLong(latitude = 50.6138111, longitude = 3.0423599)
  val lambersart           = LatLong(latitude = 50.65583909999999, longitude = 3.0226977)
  val harnes               = LatLong(latitude = 50.4515282, longitude = 2.9047234)
  val artiguesPresBordeaux = LatLong(latitude = 44.84034490000001, longitude = -0.4408037)

  /*
  Remarque Jules:
  --------------
    Les tests sont fait sur le code postal 59000.

    Pour obtenir des données à valider, effectuez la requête suivante:
      $ curl https://maps.googleapis.com/maps/api/geocode/json?components=postal_code:59000&region=eu&key=YOUR_API_KEY
   */
  "GoogleGeocoder.geocodePostalCode" should {

    def tests(implicit cache: GeoCache[LatLong]): Unit = {
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
        def testGeocoder(postalCode: PostalCode, place: LatLong): Assertion = {
          cache.flushAll()
          cache.get(postalCode) shouldBe None
          whenReady(geocoder.geocodePostalCode(postalCode).runAsync) { result =>
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
          "Artigues-près-Bordeaux" in {
            testGeocoder(PostalCode("33370"), artiguesPresBordeaux)
          }
        }
      }

      "if ALREADY in cache" should {
        val geoApiContext: GoogleGeoApiContext = GoogleGeoApiContext("WRONG KEY")
        val geocoder                           = new GoogleGeocoder(geoApiContext)

        def testGeocoder(postalCode: PostalCode, place: LatLong): Assertion = {
          cache.flushAll()
          cache.set(postalCode)(place)
          cache.get(postalCode) shouldBe Some(place)
          geocoder.geocodePostalCode(postalCode).runAsync.futureValue shouldBe place
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
          "Artigues-près-Bordeaux" in {
            testGeocoder(PostalCode("33370"), artiguesPresBordeaux)
          }
        }
      }
    }

    "With Redis" should {
      tests(RedisGeoCache[LatLong]("localhost", 6379, 1 day))
    }
    "With 'in memory' cache" should {
      tests(InMemoryGeoCache[LatLong](1 day))
    }
  }

  "GoogleGeocoder.geocodeNonAmbigueAddressT" should {

    import kantan.csv._
    import kantan.csv.generic._
    import kantan.csv.ops._

    implicitly[CellDecoder[CNil]] // IntelliJ doesn't understand that `import kantan.csv.generic._` is required.

    implicit val cache: GeoCache[LatLong] = InMemoryGeoCache[LatLong](1 day)

    final case class TestAddress(line1: String, postalCode: String, town: String, lat: String, long: String)
    object TestAddress {
      def toAddressAndLatLong(addr: TestAddress): (Address, LatLong) =
        Address(line1 = addr.line1, line2 = "", postalCode = PostalCode(addr.postalCode), town = addr.town, country = "France") -> LatLong(
          latitude = addr.lat.toDouble,
          longitude = addr.long.toDouble)
    }

    val rawData =
      s"""
        |Line1;PostalCode;Town;Lat;Long
        |5 BOULEVARD DE LA MADELEINE;75001;PARIS;48.8695813;2.3272826
        |17 rue Francois Miron;75004;Paris;48.8557984;2.3570898
        |1 RUE DANTON;75006;PARIS;48.8528005;2.3427676
        |24 rue dauphine;75006;PARIS;48.8549537;2.3393333
        |30 PLACE DE LA MADELEINE;75008;PARIS;48.8708155;2.325606
        |50 rue du Docteur Blanche;75016;Paris;48.8528274;2.2643836
        |16 RUE SAINT FIACRE  - 75002 PARIS;75002;PARIS;48.8703821;2.3459086
        |4 RUE DE SONTAY;75116;PARIS;48.8703854;2.2846272
        |7 rue Victorien Sardou;75016;Paris;48.8428041;2.2675564
        |62 avenue des champs elysee;75008;Paris;48.8708509;2.3056707
        |233 Boulevard Voltaire 75011 Paris;75011;Paris 75011;48.8512903;2.3914116
        |13 rue Henri Barbusse;92230;GENNEVILLIERS;48.9182397;2.2967879
        |35 boulevard d'Exelmans;75016;PARIS;48.84135999999999;2.2633114
        |95 avenue du General Leclerc;75014;Paris;48.8260975;2.3273668
        |12 rue de l'Assomption;75016;Paris;48.85349;2.2744602
        |108 rue de Richelieu;75002;PARIS;48.8714406;2.3398815
        |24 AVENUE MARIE ALEXIS;76370;PETIT CAUX;49.95763789999999;1.2224194
        |8 RUE FLEURS DE LYS;33370;Artigues-près-Bordeaux;44.8496786;-0.4831272
        |8 RUE des FLEURS DE LYS;33370;Artigues-près-Bordeaux;${artiguesPresBordeaux.latitude};${artiguesPresBordeaux.longitude}
        |""".stripMargin.drop(1).dropRight(1)

    val data: Seq[(Address, LatLong)] =
      rawData.unsafeReadCsv[List, TestAddress](rfc.withHeader.withCellSeparator(';')).map(TestAddress.toAddressAndLatLong)

    def testNonAmbigueAddressGeocoder: ((Address, LatLong)) => Unit = {
      case ((address: Address, latLong: LatLong)) =>
        s"$address should be located at $latLong}" in {
          geocoder.geocodeNonAmbigueAddress(address).futureValue shouldBe latLong
        }
    }

    data.foreach(testNonAmbigueAddressGeocoder.apply)
  }

}
