package com.guizmaii.distances

import com.google.maps.model.{LatLng => GoogleLatLng, TravelMode => GoogleTravelMode}
import com.guizmaii.distances.utils.circe.{LengthSerializer, ScalaDurationSerializer}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import squants.space.Length
import squants.space.LengthConversions._

import scala.collection.immutable
import scala.concurrent.duration._

object Types {

  import io.circe._
  import io.circe.generic.JsonCodec

  sealed trait Point                         extends Any
  final case class PostalCode(value: String) extends AnyVal with Point
  @JsonCodec final case class NonAmbigueAddress(line1: String, line2: String, postalCode: String, town: String, country: String)
      extends Point

  object PostalCode {
    import io.circe.generic.extras.semiauto._
    implicit val encoder: Encoder[PostalCode] = deriveUnwrappedEncoder
    implicit val decoder: Decoder[PostalCode] = deriveUnwrappedDecoder
  }

  @JsonCodec final case class LatLong(latitude: Double, longitude: Double) {
    private[distances] def asGoogleLatLng: GoogleLatLng = new GoogleLatLng(latitude, longitude)
  }

  @JsonCodec final case class Distance(length: Length, duration: Duration)

  object Distance {
    import LengthSerializer._
    import ScalaDurationSerializer._

    implicitly[Decoder[Duration]] // IntelliJ doesn't understand the need of `import ScalaDerivation._` without this
    implicitly[Decoder[Length]]   // IntelliJ doesn't understand the need of `import LengthSerializer._` without this

    final lazy val zero: Distance = Distance(0.meters, 0.seconds)
  }

  @JsonCodec final case class DirectedPath(origin: LatLong, destination: LatLong, travelModes: List[TravelMode])

  sealed trait TravelMode extends EnumEntry
  object TravelMode extends Enum[TravelMode] with CirceEnum[TravelMode] {

    val values: immutable.IndexedSeq[TravelMode] = findValues

    case object Driving   extends TravelMode
    case object Bicycling extends TravelMode
    case object Unknown   extends TravelMode

    implicit final class RichTravelMode(val travelMode: TravelMode) extends AnyVal {
      def asGoogleTravelMode: GoogleTravelMode =
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
