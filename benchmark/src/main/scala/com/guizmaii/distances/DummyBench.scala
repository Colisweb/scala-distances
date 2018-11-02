package com.guizmaii.distances

import cats.effect.{Async, IO}
import com.guizmaii.distances.Types.{LatLong, PostalCode}
import monix.eval.Task
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
class DummyBench {

  final val dummyCats  = new DummyGeoProvider[IO]
  final val dummyMonix = new DummyGeoProvider[Task]
  final val point      = PostalCode("toto")
  final val latLong    = LatLong(42, 42)

  final class DummyGeoProvider[AIO[_]](implicit AIO: Async[AIO]) extends GeoProvider[AIO] {
    override private[distances] def geocode(point: Types.Point): AIO[LatLong] = AIO.pure(latLong)
  }

  @Benchmark
  def DummyGeoProviderWithCatsEffet(): Unit = {
    dummyCats.geocode(point).unsafeRunAsync(_ => ())
  }

  import monix.execution.Scheduler.Implicits.global

  @Benchmark
  def DummyGeoProviderWithMonix(): Unit = {
    dummyMonix.geocode(point).runToFuture

    ()
  }

}
