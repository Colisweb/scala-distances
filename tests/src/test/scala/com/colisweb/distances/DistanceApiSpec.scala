package com.colisweb.distances

import cats.MonadError
import cats.effect.{ContextShift, IO}
import com.colisweb.distances.DistanceApiSpec.RunSync
import com.colisweb.distances.caches.CaffeineCache
import com.colisweb.distances.model.path.{DirectedPath, DirectedPathWithModeAt}
import com.colisweb.distances.model.{DistanceInKm, DurationInSeconds, PathResult, Point, TravelMode}
import com.colisweb.distances.providers.google._
import com.colisweb.distances.providers.here.{HereRoutingApi, HereRoutingContext, RoutingMode}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.caffeine.{CaffeineCache => CaffeineScalaCache}
import scalacache.{Flags, Mode}

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class DistanceApiSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  val runSyncTry: RunSync[Try] = new RunSync[Try] {
    override def apply[A](fa: Try[A]): A = fa.get
  }
  implicit val contextShift: ContextShift[IO] = IO.contextShift(globalExecutionContext)
  val runAsyncIO: RunSync[IO] = new RunSync[IO] {
    override def apply[A](fa: IO[A]): A = fa.unsafeRunSync()
  }
  val runAsyncMonix: RunSync[Task] = new RunSync[Task] {
    implicit val monixScheduler: Scheduler = Scheduler.global
    override def apply[A](fa: Task[A]): A  = fa.runSyncUnsafe()
  }
  private val caffeineInstance         = CaffeineScalaCache.apply[Nothing]
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

    "sync with Try" should {
      import cats.implicits.catsStdInstancesForTry
      import scalacache.modes.try_._
      "for bird distance" should {
        birdTests(runSyncTry)
      }

      "for matrix api distance" should {
        googleTests(runSyncTry, GoogleDistanceMatrixApi.sync(googleContext, TrafficModel.BestGuess))
      }

      "for direction api distance" should {
        googleTests(
          runSyncTry,
          GoogleDistanceDirectionsApi.sync(googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDistanceRoute
          )
        )
      }

      "for direction api duration" should {
        googleTests(
          runSyncTry,
          GoogleDistanceDirectionsApi.sync(googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDurationRoute
          )
        )
      }

      "for here api distance" should {
        hereTests(
          runSyncTry,
          HereRoutingApi.sync(hereContext)(
            RoutingMode.MinimalDistanceMode
          )
        )
      }

      "for here api duration" should {
        hereTests(
          runSyncTry,
          HereRoutingApi.sync(hereContext)(
            RoutingMode.MinimalDurationMode
          )
        )
      }
    }

    "async with IO" should {
      import scalacache.CatsEffect.modes.async

      "for bird distance" should {
        birdTests(runAsyncIO)
      }

      "for matrix api distance" should {
        googleTests(
          runAsyncIO,
          GoogleDistanceMatrixApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)
        )
      }

      "for direction api distance" should {
        googleTests(
          runAsyncIO,
          GoogleDistanceDirectionsApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDistanceRoute
          )
        )
      }

      "for direction api duration" should {
        googleTests(
          runAsyncIO,
          GoogleDistanceDirectionsApi.async[IO, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
            GoogleDistanceDirectionsProvider.chooseMinimalDurationRoute
          )
        )
      }

      "for here api duration" should {
        hereTests(
          runAsyncIO,
          HereRoutingApi.async[IO, DirectedPathWithModeAt](hereContext)(
            RoutingMode.MinimalDurationMode
          )
        )
      }

      "for here api distance" should {
        hereTests(
          runAsyncIO,
          HereRoutingApi.async[IO, DirectedPathWithModeAt](hereContext)(
            RoutingMode.MinimalDistanceMode
          )
        )
      }
    }

    "async with Monix Task" should {
      import scalacache.CatsEffect.modes.async

      "for bird distance" should {
        birdTests(runAsyncMonix)
      }

      "for matrix api distance" should {
        googleTests(
          runAsyncMonix,
          GoogleDistanceMatrixApi.async[Task, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)
        )
      }

      "for direction api distance" should {
        googleTests(
          runAsyncMonix,
          GoogleDistanceDirectionsApi
            .async[Task, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
              GoogleDistanceDirectionsProvider.chooseMinimalDistanceRoute
            )
        )
      }

      "for direction api duration" should {
        googleTests(
          runAsyncMonix,
          GoogleDistanceDirectionsApi
            .async[Task, DirectedPathWithModeAt](googleContext, TrafficModel.BestGuess)(
              GoogleDistanceDirectionsProvider.chooseMinimalDurationRoute
            )
        )
      }

      "for here api duration" should {
        hereTests(
          runAsyncMonix,
          HereRoutingApi
            .async[Task, DirectedPathWithModeAt](hereContext)(
              RoutingMode.MinimalDurationMode
            )
        )
      }

      "for here api distance" should {
        hereTests(
          runAsyncMonix,
          HereRoutingApi
            .async[Task, DirectedPathWithModeAt](hereContext)(
              RoutingMode.MinimalDistanceMode
            )
        )
      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    import scalacache.modes.try_._

    // clear caches
    caffeineInstance.doRemoveAll().get
    ()
  }

  private def birdTests[F[_]](run: RunSync[F])(implicit
      F: MonadError[F, Throwable],
      mode: Mode[F]
  ): Unit = {
    "Bird only" should {
      val distanceApi = Distances.haversine[F, DirectedPathWithModeAt].api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        None,
        run
      )
    }

    "Bird with Caffeine cache" should {
      val distanceApi = Distances
        .haversine[F, DirectedPathWithModeAt]
        .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = None,
        run
      )
    }
  }

  private def googleTests[F[_]](
      run: RunSync[F],
      googleApi: DistanceApi[F, DirectedPathWithModeAt]
  )(implicit
      F: MonadError[F, Throwable],
      mode: Mode[F]
  ): Unit = {
    "Google api only" should {
      relativeTests(
        googleApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        googleApi,
        googleResults,
        trafficTime = None,
        run
      )
    }

    "Google api with Caffeine cache" should {
      val distanceApi = Distances
        .from(googleApi)
        .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        googleResults,
        trafficTime = None,
        run
      )
    }

    "Google api with fallback on Bird, and traffic in the past" should {
      val distanceApi = Distances
        .from(googleApi)
        .fallback(Distances.haversine[F, DirectedPathWithModeAt])
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime),
        run
      )
      approximateTests(
        distanceApi,
        birdResults,
        trafficTime = Some(pastTime),
        run
      )
    }

    "Google api with departureTime correction and traffic in the past" should {
      val distanceApi = Distances
        .from(googleApi)
        .correctPastDepartureTime(1.hour)
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime),
        run
      )
      approximateTests(
        distanceApi,
        googleResults,
        trafficTime = Some(pastTime),
        run
      )
    }

  }

  private def approximateTests[F[_]](
      api: DistanceApi[F, DirectedPathWithModeAt],
      results: Map[(Point, Point), (DistanceInKm, DurationInSeconds)],
      trafficTime: Option[Instant],
      run: RunSync[F],
      checkPolyline: Boolean = false
  ): Unit = {

    "return approximate distance and duration from Paris 01 to Marseille 01" in {
      val driveFrom01to02 = DirectedPathWithModeAt(
        origin = paris01,
        destination = marseille01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )
      val distanceFrom01to02   = run(api.distance(driveFrom01to02))
      val (distance, duration) = results(paris01 -> marseille01)

      if (checkPolyline)
        distanceFrom01to02.paths should not be empty

      distanceFrom01to02.distance shouldBe distance +- distance / 8
      distanceFrom01to02.duration shouldBe duration +- duration / 8
    }
  }

  private def relativeTests[F[_]](
      api: DistanceApi[F, DirectedPathWithModeAt],
      trafficTime: Option[Instant],
      run: RunSync[F],
      checkPolyline: Boolean = false
  ): Unit = {

    "return zero between the same points" in {
      val path = DirectedPathWithModeAt(
        origin = paris01,
        destination = paris01,
        travelMode = TravelMode.Car(50.0),
        departureTime = trafficTime
      )

      val distance = run(api.distance(path))

      if (checkPolyline)
        distance.paths should not be empty

      distance shouldBe PathResult(0d, 0, List(DirectedPath(paris01, paris01)))
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

      val distanceFromP01toM01   = run(api.distance(driveFromP01toM01))
      val distanceFromRouenToM01 = run(api.distance(driveFromRouenToM01))

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

      val distanceWithoutTraffic = run(api.distance(pathWithoutTraffic))
      val distanceWithTraffic    = run(api.distance(pathWithTraffic))

      distanceWithoutTraffic.duration should be <= distanceWithTraffic.duration + 60
    }
  }

  private def hereTests[F[_]](
      run: RunSync[F],
      hereApi: DistanceApi[F, DirectedPathWithModeAt]
  )(implicit
      F: MonadError[F, Throwable],
      mode: Mode[F]
  ): Unit = {
    "Here api only" should {
      relativeTests(
        hereApi,
        trafficTime = Some(futureTime),
        run,
        checkPolyline = true
      )
      approximateTests(
        hereApi,
        hereResults,
        trafficTime = None,
        run,
        checkPolyline = true
      )
    }

    "Here api with Caffeine cache" should {
      val distanceApi = Distances
        .from(hereApi)
        .caching(CaffeineCache.apply(Flags.defaultFlags, Some(1 days)))
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(futureTime),
        run
      )
      approximateTests(
        distanceApi,
        hereResults,
        trafficTime = None,
        run
      )
    }

    "Here api with traffic in the past" should {
      val distanceApi = Distances
        .from(hereApi)
        .fallback(Distances.haversine[F, DirectedPathWithModeAt])
        .api
      relativeTests(
        distanceApi,
        trafficTime = Some(pastTime),
        run
      )
    }

  }
}

object DistanceApiSpec {

  trait RunSync[F[_]] {
    def apply[A](fa: F[A]): A
  }
}
