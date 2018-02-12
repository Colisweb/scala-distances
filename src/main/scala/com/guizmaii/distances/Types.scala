package com.guizmaii.distances

import com.google.maps.model.{LatLng => GoogleLatLng, TravelMode => GoogleTravelMode}
import enumeratum.{Enum, EnumEntry}
import squants.space.Length
import squants.space.LengthConversions._

import scala.collection.immutable
import scala.concurrent.duration._

object Types {

  type CacheableDistance = ((TravelMode, LatLong, LatLong), SerializableDistance)

  // TODO Jules: This class should not leak out of this project. // private[distances]
  final case class SerializableDistance(value: Double, duration: Double)

  final case class PostalCode(value: String) extends AnyVal
  final case class Address(line1: String, line2: String, postalCode: PostalCode, town: String, country: String)

  final case class LatLong(latitude: Double, longitude: Double) {
    private[distances] def toGoogleLatLng: GoogleLatLng = new GoogleLatLng(latitude, longitude)
  }

  final case class Distance(length: Length, duration: Duration)

  object Distance {
    private[distances] def apply(s: SerializableDistance): Distance =
      Distance(length = s.value meters, duration = s.duration seconds)

    final lazy val zero: Distance = Distance(0 meters, 0 seconds)
    final lazy val Inf: Distance  = Distance(Double.PositiveInfinity meters, Duration.Inf)
  }

  final case class DirectedPath(origin: LatLong, destination: LatLong, travelModes: List[TravelMode])

  sealed trait TravelMode extends EnumEntry
  object TravelMode extends Enum[TravelMode] {

    val values: immutable.IndexedSeq[TravelMode] = findValues

    case object Driving   extends TravelMode
    case object Bicycling extends TravelMode
    case object Unknown   extends TravelMode

    implicit final class RichTravelMode(val travelMode: TravelMode) extends AnyVal {
      def toGoogleTravelMode: GoogleTravelMode =
        travelMode match {
          case Driving   => GoogleTravelMode.DRIVING
          case Bicycling => GoogleTravelMode.BICYCLING
          case Unknown   => GoogleTravelMode.UNKNOWN
        }
    }

    implicit final class RichGoogleTravelMode(val travelMode: GoogleTravelMode) extends AnyVal {

      /**
        * For now, I don't want to handle WALKING and TRANSIT.
        *
        * @return
        */
      def fromGoogleTravelMode: TravelMode = {
        import GoogleTravelMode._

        travelMode match {
          case DRIVING                     => Driving
          case BICYCLING                   => Bicycling
          case UNKNOWN | WALKING | TRANSIT => Unknown
        }
      }
    }

  }

}
