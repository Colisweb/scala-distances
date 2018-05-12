package com.guizmaii.distances.providers

import java.util.UUID

import cats.effect.{Async, IO}
import com.guizmaii.distances.Types._
import com.guizmaii.distances.providers.RedisCacheProvider.RedisConfiuration
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
    directedPath: DirectedPath,
    postalCode: PostalCode,
    nonAmbigueAddress: NonAmbigueAddress,
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

  def expectedJson(toto: Toto): Json =
    json"""
      {
        "name" : ${toto.name},
        "age" : ${toto.age},
        "directedPath" : {
          "origin" : {
            "latitude" : ${toto.directedPath.origin.latitude},
            "longitude" : ${toto.directedPath.origin.longitude}
          },
          "destination" : {
            "latitude" : ${toto.directedPath.destination.latitude},
            "longitude" : ${toto.directedPath.destination.longitude}
          },
          "travelModes" : ${toto.directedPath.travelModes}
        },
        "postalCode" : ${toto.postalCode.value},
        "nonAmbigueAddress" : {
          "line1" : ${toto.nonAmbigueAddress.line1},
          "line2" : ${toto.nonAmbigueAddress.line2},
          "postalCode" : ${toto.nonAmbigueAddress.postalCode},
          "town" : ${toto.nonAmbigueAddress.town},
          "country" : ${toto.nonAmbigueAddress.country}
        },
        "distance" : {
          "length" : ${toto.distance.length},
          "duration" : ${toto.distance.duration}
        }
      }
    """

  def tests[AIO[+ _]: Async](cacheImpl: () => CacheProvider[AIO])(runSync: AIO[Any] => Any): Unit = {
    val cache = cacheImpl()
    val key   = UUID.randomUUID()

    implicit final class RichCacheProvider(val cache: CacheProvider[AIO]) {
      import scalacache.CatsEffect.modes.async

      def removeAll(): AIO[Any] = cache.innerCache.removeAll[AIO]()(async[AIO])
    }

    "empty cache" should {
      "returns nothing" in {
        runSync(cache.get[Toto](key)).asInstanceOf[Option[Toto]] shouldBe empty
      }
    }
    "cache" should {
      "save things" in {
        forAll(totoGen) { toto: Toto =>
          runSync(cache.set(key)(toto, Some(1 day))) shouldBe expectedJson(toto)
          runSync(cache.get[Toto](key)) shouldBe Some(toto)
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
      tests[IO](() => RedisCacheProvider(RedisConfiuration("127.0.0.1", 6379), Some(1 day)))(_.unsafeRunSync())
    }
  }
  "with Monix Task" should {
    import monix.execution.Scheduler.Implicits.global

    "with InMemoryCacheProvider" should {
      tests[Task](() => InMemoryCacheProvider(Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
    "pass RedisCacheProvider" should {
      tests[Task](() => RedisCacheProvider(RedisConfiuration("127.0.0.1", 6379), Some(1 day)))(_.runSyncUnsafe(10 seconds))
    }
  }

}
