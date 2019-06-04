package com.guizmaii.distances.caches

import cats.effect.{Async, IO}
import com.guizmaii.distances.Cache
import com.guizmaii.distances.Types._
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

class CacheSpec extends WordSpec with Matchers with PropertyChecks {

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

  def tests[F[+ _]](cacheImpl: () => Cache[F])(runSync: F[Any] => Any)(implicit F: Async[F]): Unit = {
    val cache = cacheImpl()

    implicit final class RichCacheProvider(val cache: Cache[F]) {
      import scalacache.CatsEffect.modes.async

      def removeAll(): F[Any] = cache.innerCache.removeAll[F]()(async[F])
    }

    "cache" should {
      "save things" in {
        forAll(travelModeGen, latLongGen, latLongGen, distaceGen) { (mode, origin, destination, distance) =>
          runSync(cache.cachingF(F.pure(distance), Distance.decoder, Distance.encoder, mode, origin, destination)) shouldBe distance
          runSync(
            cache.cachingF(
              F.raiseError(new RuntimeException).asInstanceOf[F[Distance]],
              Distance.decoder,
              Distance.encoder,
              mode,
              origin,
              destination
            )
          ) shouldBe distance
          runSync(cache.removeAll())
        }
      }
    }
  }

  "with cats-effect IO" should {
    "with CaffeineCache" should {
      tests[IO](() => CaffeineCache(Some(1 day)))(_.unsafeRunSync())
    }
    "pass RedisCache" should {
      tests[IO](() => RedisCache(RedisConfiguration("127.0.0.1", 6379), Some(1 day)))(_.unsafeRunSync())
    }
  }
  "with Monix Task" should {
    import monix.execution.Scheduler.Implicits.global

    "with CaffeineCache" should {
      tests[Task](() => CaffeineCache(Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
    "pass RedisCache" should {
      tests[Task](() => RedisCache(RedisConfiguration("127.0.0.1", 6379), Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
  }

}
