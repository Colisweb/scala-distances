package com.colisweb.distances

import java.time.Instant

import cats.effect.{Concurrent, ContextShift, IO}
import com.colisweb.distances.TravelMode._
import com.colisweb.distances.Types._
import com.colisweb.distances.caches.{CaffeineCache, RedisCache, RedisConfiguration}
import com.colisweb.distances.providers.google.GoogleGeoApiContext
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import squants.space.LengthConversions._
import squants.space.Meters

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DistanceApiSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  import cats.temp.par._
  import com.colisweb.distances.utils.Stubs._

  val globalExecutionContext: ExecutionContext = ExecutionContext.global
  implicit val contextShift: ContextShift[IO]  = IO.contextShift(globalExecutionContext)

  val loggingF: String => Unit = (s: String) => println(s.replaceAll("key=([^&]*)&", "key=REDACTED&"))

  lazy val geoContext: GoogleGeoApiContext = GoogleGeoApiContext(System.getenv().get("GOOGLE_API_KEY"), loggingF)

  "DistanceApi" should {
    "#distance" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cache          = CaffeineCache[IO](Some(1 days))
          val stub           = distanceProviderStub[IO, Unit]
          val distanceApi    = DistanceApi[IO, Unit](stub.distance, stub.batchDistances, cache.caching, cache.get)
          val latLong        = LatLong(0.0, 0.0)
          val expectedResult = Map((Driving, Right(Distance.zero)), (Bicycling, Right(Distance.zero)))

          distanceApi.distance(latLong, latLong, Driving :: Bicycling :: Nil).unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distanceFromPostalCodes" should {
      "if origin == destination" should {
        "not call the provider and return immmediatly Distance.zero" in {
          val cache          = CaffeineCache[IO](Some(1 days))
          val stub           = distanceProviderStub[IO, Unit]
          val distanceApi    = DistanceApi[IO, Unit](stub.distance, stub.batchDistances, cache.caching, cache.get)
          val postalCode     = PostalCode("59000")
          val expectedResult = Map((Driving, Right(Distance.zero)), (Bicycling, Right(Distance.zero)))

          distanceApi
            .distanceFromPostalCodes(geocoderStub)(postalCode, postalCode, Driving :: Bicycling :: Nil)
            .unsafeRunSync() shouldBe expectedResult
        }
      }
    }

    "#distances" should {
      "pass the same test suite than GoogleDistanceProvider" should {
        def passTests[F[+ _]: Concurrent: Par](runSync: F[Any] => Any, cache: Cache[F]): Unit = {
          val distanceApi: DistanceApi[F, Unit] =
            DistanceApi[F, Unit](mockedDistanceF[F], mockedBatchDistanceF[F], cache.caching, cache.get)

          val paris01 = LatLong(48.8640493, 2.3310526)
          val paris02 = LatLong(48.8675641, 2.34399)
          val paris18 = LatLong(48.891305, 2.3529867)

          "says that Paris 02 is nearest to Paris 01 than Paris 18" in {
            val driveFrom01to02 = DirectedPathMultipleModes(origin = paris01, destination = paris02, Driving :: Nil)
            val driveFrom01to18 = DirectedPathMultipleModes(origin = paris01, destination = paris18, Driving :: Nil)

            val results = runSync(distanceApi.distances(Array(driveFrom01to02, driveFrom01to18)))
              .asInstanceOf[Map[DirectedPath, Either[Unit, Distance]]]

            // We only check the length as travel duration varies over time & traffic
            results.mapValues(_.right.get.length) shouldBe Map(
              DirectedPath(paris01, paris02, Driving, None) -> 1024.meters,
              DirectedPath(paris01, paris18, Driving, None) -> 3429.meters
            )

            results(DirectedPath(paris01, paris02, Driving, None)).right.get.length should be <
              results(DirectedPath(paris01, paris18, Driving, None)).right.get.length

            results(DirectedPath(paris01, paris02, Driving, None)).right.get.duration should be <
              results(DirectedPath(paris01, paris18, Driving, None)).right.get.duration
          }

          val origin         = LatLong(48.8640493, 2.3310526)
          val destination    = LatLong(48.8675641, 2.34399)
          val length         = Meters(1024)
          val travelDuration = 73728.millis

          "not take into account traffic when not asked to with a driving travel mode" in {
            val result = Map(Driving -> Right(Distance(length, travelDuration)))

            runSync(distanceApi.distance(origin, destination, Driving :: Nil, None)) shouldBe result
          }

          "take into account traffic when asked to with a driving travel mode and a best guess estimation" in {
            val result          = Map(Driving -> Right(Distance(length, travelDuration + 5.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.BestGuess)

            runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling))) shouldBe result
          }

          "take into account traffic when asked to with a driving travel mode and an optimistic estimation" in {
            val result          = Map(Driving -> Right(Distance(length, travelDuration + 2.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.Optimistic)

            runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling))) shouldBe result
          }

          "take into account traffic when asked to with a driving travel mode and a pessimistic estimation" in {
            val result          = Map(Driving -> Right(Distance(length, travelDuration + 10.minutes)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.Pessimistic)

            runSync(distanceApi.distance(origin, destination, Driving :: Nil, Some(trafficHandling))) shouldBe result
          }

          "not take into account traffic when asked to with a bicycling travel mode" in {
            val result          = Map(Bicycling -> Right(Distance(length, travelDuration)))
            val trafficHandling = TrafficHandling(Instant.now, TrafficModel.BestGuess)

            runSync(distanceApi.distance(origin, destination, Bicycling :: Nil, Some(trafficHandling))) shouldBe result
          }

          "say that Paris 02 is closer to Paris 01 than Paris 18 in a batch distances computation" in {
            val results =
              runSync(distanceApi.batchDistances(List(paris01), List(paris02, paris18), Driving, None))
                .asInstanceOf[Map[(LatLong, LatLong), Either[Unit, Distance]]]

            results(paris01 -> paris02).right.get.length should be < results(paris01 -> paris18).right.get.length

            results(paris01 -> paris02).right.get.duration should be < results(paris01 -> paris18).right.get.duration
          }

          "return all the uncached distances in a batch distances computation" in {
            val origins      = List(LatLong(48.86, 2.33), LatLong(48.87, 2.34), LatLong(48.88, 2.35))
            val destinations = List(LatLong(48.85, 2.32), LatLong(48.84, 2.31), LatLong(48.83, 2.3))
            val pairs        = origins.flatMap(origin => destinations.map(origin -> _))

            val results =
              runSync(distanceApi.batchDistances(origins, destinations, Driving, None))
                .asInstanceOf[Map[DirectedPath, Either[Unit, Distance]]]

            val caches = pairs.map {
              case (origin, destination) =>
                runSync(cache.get(Distance.decoder, Driving, origin, destination, None))
                  .asInstanceOf[Option[Distance]]
            }

            results.keys should contain theSameElementsAs pairs
            results.values.forall(_.isRight) shouldBe true
            caches.forall(_.isDefined) shouldBe true

            val moreOrigins      = List(LatLong(48.85, 2.33), LatLong(48.85, 2.34))
            val moreDestinations = List(LatLong(48.84, 2.36), LatLong(48.83, 2.37))
            val allOrigins       = origins ++ moreOrigins
            val allDestinations  = destinations ++ moreDestinations
            val allPairs         = allOrigins.flatMap(origin => allDestinations.map(origin -> _))

            val allResults =
              runSync(distanceApi.batchDistances(allOrigins, allDestinations, Driving, None))
                .asInstanceOf[Map[DirectedPath, Either[Unit, Distance]]]

            val allCaches = allPairs.map {
              case (origin, destination) =>
                runSync(cache.get(Distance.decoder, Driving, origin, destination, None))
                  .asInstanceOf[Option[Distance]]
            }

            allResults.keys should contain theSameElementsAs allPairs
            allResults.values.forall(_.isRight) shouldBe true
            allCaches.forall(_.isDefined) shouldBe true
          }
        }

        "pass tests with cats-effect and Caffeine" should {
          passTests[IO](_.unsafeRunTimed(10 seconds).get, CaffeineCache[IO](Some(1 days)))
        }

        "pass tests with cats-effect and Redis" should {
          passTests[IO](
            _.unsafeRunTimed(10 seconds).get,
            RedisCache[IO](RedisConfiguration("localhost", 6379), Some(1 days))
          )
        }

        "pass tests with Monix and Caffeine" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](_.runSyncUnsafe(10 seconds), CaffeineCache[Task](Some(1 days)))
        }

        "pass tests with Monix and Redis" should {
          import monix.execution.Scheduler.Implicits.global

          passTests[Task](
            _.runSyncUnsafe(10 seconds),
            RedisCache[Task](RedisConfiguration("localhost", 6379), Some(1 days))
          )
        }
      }
    }

  }

}
