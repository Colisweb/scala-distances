package com.guizmaii.distances

import squants.space.Length
import squants.space.LengthConversions._

import scala.concurrent.duration._

object Types {

  // TODO Jules: This class should not leak out of this project. // private[distances]
  final case class SerializableDistance(value: Double, duration: Double)

  final case class PostalCode(value: String) extends AnyVal

  final case class LatLong(latitude: Double, longitude: Double)

  final case class Distance(length: Length, duration: Duration) extends Ordered[Distance] {
    override def compare(that: Distance): Int = this.length.compareTo(that.length)
  }

  object Distance {
    private[distances] def apply(s: SerializableDistance): Distance =
      Distance(length = s.value meters, duration = s.duration seconds)

    final lazy val zero: Distance = Distance(0 meters, 0 seconds)
    final lazy val Inf: Distance  = Distance(Double.PositiveInfinity meters, Duration.Inf)
  }

  type DirectedPath             = (LatLong, LatLong)
  type DirectedPathWithDistance = (LatLong, LatLong, Distance)

}
