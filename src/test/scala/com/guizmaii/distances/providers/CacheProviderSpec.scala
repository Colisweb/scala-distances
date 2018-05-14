package com.guizmaii.distances.providers

import cats.effect.{Async, IO}
import com.guizmaii.distances.Types._
import com.guizmaii.distances.providers.RedisCacheProvider.RedisConfiguration
import io.circe._
import io.circe.generic.semiauto._
import monix.eval.Task
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import squants.space.Length

import scala.concurrent.duration._
import scala.language.postfixOps

final case class Toto(
    name: String,
    age: Int,
    latLong: LatLong,
    distance: Distance
)

object Toto {
  implicit final val decoder: Decoder[Toto] = deriveDecoder[Toto]
  implicit final val Encoder: Encoder[Toto] = deriveEncoder[Toto]
}

class CacheProviderSpec extends WordSpec with Matchers with PropertyChecks {

  import com.guizmaii.distances.generators.Gens._
  import com.guizmaii.distances.utils.circe.LengthSerializer._
  import com.guizmaii.distances.utils.circe.ScalaDurationSerializer._
  import io.circe.literal._

  implicitly[Decoder[Duration]] // IntelliJ doesn't understand the need of `import ScalaDerivation._` without this
  implicitly[Decoder[Length]]   // IntelliJ doesn't understand the need of `import LengthSerializer._` without this

  // TODO Jules: Implement test for JSON serialization
  def expectedJson(toto: Toto): Json =
    json"""
      {
        "name" : ${toto.name},
        "age" : ${toto.age},
        "latLong" : {
          "latitude" : ${toto.latLong.latitude},
          "longitude" : ${toto.latLong.longitude}
        },
        "distance" : {
          "length" : ${toto.distance.length},
          "duration" : ${toto.distance.duration}
        }
      }
    """

  def tests[AIO[+ _]](cacheImpl: () => CacheProvider[AIO])(runSync: AIO[Any] => Any)(implicit AIO: Async[AIO]): Unit = {
    val cache = cacheImpl()

    implicit final class RichCacheProvider(val cache: CacheProvider[AIO]) {
      import scalacache.CatsEffect.modes.async

      def removeAll(): AIO[Any] = cache.innerCache.removeAll[AIO]()(async[AIO])
    }

    "cache" should {
      "save things" in {
        forAll(travelModeGen, latLongGen, latLongGen, distaceGen) { (mode, origin, destination, distance) =>
          runSync(cache.cachingF(mode, origin, destination)(AIO.pure(distance))) shouldBe distance
          runSync(cache.cachingF(mode, origin, destination)(AIO.raiseError(new RuntimeException).asInstanceOf[AIO[Distance]])) shouldBe distance
          runSync(cache.removeAll())
        }
      }
    }
  }

  "with cats-effect IO" should {
    "with InMemoryCacheProvider" should {
      tests[IO](() => InMemoryCacheProvider(Some(1 day)))(_.unsafeRunSync())
    }
    "pass RedisCacheProvider" should {
      tests[IO](() => RedisCacheProvider(RedisConfiguration("127.0.0.1", 6379), Some(1 day)))(_.unsafeRunSync())
    }
  }
  "with Monix Task" should {
    import monix.execution.Scheduler.Implicits.global

    "with InMemoryCacheProvider" should {
      tests[Task](() => InMemoryCacheProvider(Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
    "pass RedisCacheProvider" should {
      tests[Task](() => RedisCacheProvider(RedisConfiguration("127.0.0.1", 6379), Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
  }

}
