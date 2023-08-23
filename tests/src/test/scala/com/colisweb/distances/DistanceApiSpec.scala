package com.colisweb.distances

import cats.implicits._
import cats.effect.{ContextShift, IO}
import com.colisweb.distances.DistanceApiSpec.pathResultCodec
import com.colisweb.distances.caches.RedisCache
import com.colisweb.distances.model.path.DirectedPathWithModeAt
import com.colisweb.distances.model.{DistanceInKm, DurationInSeconds, PathResult, Point, TravelMode}
import com.colisweb.distances.providers.google._
import com.colisweb.distances.providers.here.{HereRoutingApi, HereRoutingContext, RoutingMode}
import com.colisweb.simplecache.wrapper.cats.CatsCache
import io.circe.Codec
import io.circe.generic.extras.{Configuration => CirceConfiguration}
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DistanceApiSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  implicit val contextShift: ContextShift[IO]  = IO.contextShift(globalExecutionContext)

  private val configuration            = Configuration.load
  private val loggingF: String => Unit = (s: String) => println(s.replaceAll("key=([^&]*)&", "key=REDACTED&"))
  private val googleContext: GoogleGeoApiContext =
    new GoogleGeoApiContext(configuration.google.apiKey, 10 second, 60 second, 1000, loggingF)
  private val hereContext: HereRoutingContext = HereRoutingContext(configuration.here.apiKey, 10 second, 60 second)
  private val futureTime                      = ZonedDateTime.now().plusHours(1).toInstant
  private val pastTime                        = ZonedDateTime.now().minusHours(1).toInstant

  private val paris01     = Point(48.8640493, 2.3310526, Some(79d))
  private val paris18     = Point(48.891305, 2.3529867, Some(79d))
  private val rouen       = Point(49.443232, 1.099971)
  private val marseille01 = Point(43.2969901, 5.3789783)

  private val redisCache1Day =
    RedisCache[DirectedPathWithModeAt, PathResult](configuration.redis.asConfiguration, Some(1 days))
  private val catsRedisCache1Day: CatsCache[IO, DirectedPathWithModeAt, PathResult] = CatsCache(redisCache1Day)

  private val birdResults = Map(
    (paris01 -> paris18, (3.4, 246L)),
    (paris01 -> marseille01, (661.91, 47665L))
  )

  private val googleResults = Map(
    (paris01 -> paris18, (4.5, 1075L)),
    (paris01 -> marseille01, (779.0, 27133L))
  )

  private val hereResults = Map(
    (paris01 -> paris18, (4.5, 700L)),
    (paris01 -> marseille01, (778.0, 58233L))
  )

  "DistanceApi" should {

    "async with IO" should {

      "for bird distance" should {
        birdTests()
      }

      "for matrix api distance" should {
        googleTests(
          GoogleDistanceMatrixApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)
        )
      }

      "for direction api distance" should {
        googleTests(
          GoogleDistanceDirectionsApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDistanceRoute
          )
        )
      }

      "for direction api duration" should {
        googleTests(
          GoogleDistanceDirectionsApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDurationRoute
          )
        )
      }

      "for here api duration" should {
        hereTests(
          HereRoutingApi.async[IO, DirectedPathWithModeAt](hereContext)(
            RoutingMode.MinimalDurationMode
          )
        )
      }

      "for here api distance" should {
        hereTests(
          HereRoutingApi.async[IO, DirectedPathWithModeAt](hereContext)(
            RoutingMode.MinimalDistanceMode
          )
        )
      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    // clear caches
    // catsRedisCache1Day.clear().unsafeRunSync()
    ()
  }

  private def birdTests(): Unit = {
    "Bird only" should {
      val distanceApi = Distances.haversine[IO, DirectedPathWithModeAt].api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime)
      )
      approximateTests(
        distanceApi,
        birdResults,
        None
      )
    }

    "Bird with Redis cache" should {
      val distanceApi = Distances
        .haversine[IO, DirectedPathWithModeAt]
        .caching(catsRedisCache1Day)
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime)
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = None
      )
    }
  }

  private def googleTests(googleApi: DistanceApi[IO, DirectedPathWithModeAt]): Unit = {
    "Google api only" should {
      relativeTests(
        googleApi,
        trafficTime = Some(futureTime)
      )
      approximateTests(
        googleApi,
        googleResults,
        trafficTime = None
      )
    }

    "Google api with Caffeine cache" should {
      val distanceApi = Distances
        .from(googleApi)
        .caching(catsRedisCache1Day)
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime)
      )
      approximateTests(
        distanceApi,
        googleResults,
        trafficTime = None
      )
    }

    "Google api with fallback on Bird, and traffic in the past" should {
      val distanceApi = Distances
        .from(googleApi)
        .fallback(Distances.haversine[IO, DirectedPathWithModeAt])
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime)
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = Some(pastTime)
      )
    }

    "Google api with departureTime correction and traffic in the past" should {
      val distanceApi = Distances
        .from(googleApi)
        .correctPastDepartureTime(1.hour)
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime)
      )
      approximateTests(
        distanceApi,
        googleResults,
        trafficTime = Some(pastTime)
      )
    }

  }

  private def approximateTests(
      api: DistanceApi[IO, DirectedPathWithModeAt],
      results: Map[(Point, Point), (DistanceInKm, DurationInSeconds)],
      trafficTime: Option[Instant],
      checkElevationProfile: Boolean = false
  ): Unit = {

    "return approximate distance and duration from Paris 01 to Marseille 01" in {
      val driveFrom01to02 = DirectedPathWithModeAt(
        origin = paris01,
        destination = marseille01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )
      val distanceFrom01to02   = api.distance(driveFrom01to02).unsafeRunSync()
      val (distance, duration) = results(paris01 -> marseille01)

      if (checkElevationProfile)
        distanceFrom01to02.elevationProfile should not be empty

      distanceFrom01to02.distance shouldBe distance +- distance / 8
      distanceFrom01to02.duration shouldBe duration +- duration / 8
    }
  }

  private def relativeTests(
      api: DistanceApi[IO, DirectedPathWithModeAt],
      trafficTime: Option[Instant],
      checkElevationProfile: Boolean = false
  ): Unit = {

    "return zero between the same points" in {
      val path = DirectedPathWithModeAt(
        origin = paris01,
        destination = paris01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )

      val distance = api.distance(path).unsafeRunSync()

      if (checkElevationProfile)
        distance.elevationProfile should not be empty

      distance.distance shouldBe 0d
      distance.duration shouldBe 0
    }

    "return smaller  from Paris 01 to Marseille 01 than from Rouen to Marseille" in {
      val driveFromP01toM01 = DirectedPathWithModeAt(
        origin = paris01,
        destination = marseille01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )
      val driveFromRouenToM01 = DirectedPathWithModeAt(
        origin = rouen,
        destination = marseille01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )

      val distanceFromP01toM01   = api.distance(driveFromP01toM01).unsafeRunSync()
      val distanceFromRouenToM01 = api.distance(driveFromRouenToM01).unsafeRunSync()

      distanceFromP01toM01.distance should be < distanceFromRouenToM01.distance
      distanceFromP01toM01.duration should be < distanceFromRouenToM01.duration
    }

    // NB: Distance maybe longer, but Duration should be smaller
    "return smaller or equal Duration with traffic in Paris" in {
      val pathWithoutTraffic = DirectedPathWithModeAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Car(50.0),
        departureTime = None
      )
      val pathWithTraffic = DirectedPathWithModeAt(
        origin = paris01,
        destination = paris18,
        travelMode = TravelMode.Car(50.0),
        departureTime = Some(futureTime)
      )

      val distanceWithoutTraffic = api.distance(pathWithoutTraffic).unsafeRunSync()
      val distanceWithTraffic    = api.distance(pathWithTraffic).unsafeRunSync()

      distanceWithoutTraffic.duration should be <= distanceWithTraffic.duration + 60
    }
  }

  private def hereTests(hereApi: DistanceApi[IO, DirectedPathWithModeAt]): Unit = {
    "Here api only" should {
      relativeTests(
        hereApi,
        trafficTime = Some(futureTime),
        checkElevationProfile = true
      )
      approximateTests(
        hereApi,
        hereResults,
        trafficTime = None,
        checkElevationProfile = true
      )
    }

    "Here api with Caffeine cache" should {
      val distanceApi = Distances
        .from(hereApi)
        .caching(catsRedisCache1Day)
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime)
      )
      approximateTests(
        distanceApi,
        hereResults,
        trafficTime = None
      )
    }

    "Here api with traffic in the past" should {
      val distanceApi = Distances
        .from(hereApi)
        .fallback(Distances.haversine[IO, DirectedPathWithModeAt])
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime)
      )
    }

  }
}

object DistanceApiSpec {
  implicit val customConfig: CirceConfiguration   = CirceConfiguration.default.withDefaults
  implicit val pathResultCodec: Codec[PathResult] = deriveConfiguredCodec
}
