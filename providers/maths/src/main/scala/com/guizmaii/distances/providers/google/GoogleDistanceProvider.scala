package com.guizmaii.distances.providers.google

import cats.effect.Async
import com.guizmaii.distances.DistanceProvider
import com.guizmaii.distances.Types.TravelMode.{Bicycling, Driving, Unknown}
import com.guizmaii.distances.Types.{Distance, LatLong, TravelMode}
import squants.motion.Velocity
import squants.space.LengthConversions._
import squants.time.TimeConversions._

import scala.concurrent.duration._
import scala.language.postfixOps

object GoogleDistanceProvider {

  final def speed(travelMode: TravelMode): Velocity = travelMode match {
    case Driving   => 50.km / 1.hour
    case Bicycling => 15.km / 1.hour
    case Unknown   => throw new RuntimeException("not yet handled")
  }

  final def apply[AIO[_]]()(implicit AIO: Async[AIO]): DistanceProvider[AIO] = new DistanceProvider[AIO] {

    override private[distances] final def distance(mode: TravelMode, origin: LatLong, destination: LatLong): AIO[Distance] = {
      val deltaLat  = math.toRadians(destination.latitude - origin.latitude)
      val deltaLong = math.toRadians(destination.longitude - origin.longitude)
      val a = math.pow(math.sin(deltaLat / 2), 2) + math.cos(math.toRadians(origin.latitude)) * math.cos(
        math.toRadians(destination.latitude)) * math.pow(math.sin(deltaLong / 2), 2)
      val greatCircleDistance = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
      val length = (3958.761 / 0.00062137 * greatCircleDistance).meters
      val duration = length.toKilometers * speed(mode).toKilometersPerHour

      AIO.pure(Distance(length = length, duration = duration.hours))
    }

  }

}
