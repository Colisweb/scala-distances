package com.guizmaii.distances

import cats.Show
import com.guizmaii.distances.utils.circe.{LengthSerializer, ScalaDurationSerializer}
import enumeratum.{Enum, EnumEntry}
import squants.space.Length

import scala.collection.immutable
import scala.concurrent.duration._

object Types {

  import io.circe._
  import io.circe.generic.semiauto._
  import squants.space.LengthConversions._

  sealed trait Point                                                                                                    extends Any
  final case class PostalCode(value: String)                                                                            extends AnyVal with Point
  final case class NonAmbiguousAddress(line1: String, line2: String, postalCode: String, town: String, country: String) extends Point

  object PostalCode {
    implicit final val show: Show[PostalCode] =
      new Show[PostalCode] {
        override def show(p: PostalCode): String = p.value
      }
  }

  object NonAmbiguousAddress {
    implicit final val show: Show[NonAmbiguousAddress] =
      new Show[NonAmbiguousAddress] {
        override def show(a: NonAmbiguousAddress): String = s"${a.line1}, ${a.line2}, ${a.postalCode} ${a.town} ${a.country}"
      }
  }

  final case class LatLong(latitude: Double, longitude: Double)

  object LatLong {
    private[distances] implicit final val encoder: Encoder[LatLong] = deriveEncoder[LatLong]
    private[distances] implicit final val decoder: Decoder[LatLong] = deriveDecoder[LatLong]
  }

  final case class Distance(length: Length, duration: Duration)

  object Distance {
    import LengthSerializer._
    import ScalaDurationSerializer._

    final lazy val zero: Distance = Distance(0.meters, 0.seconds)
    final lazy val Inf: Distance  = Distance(Double.PositiveInfinity.meters, Duration.Inf)

    implicitly[Decoder[Duration]] // IntelliJ doesn't understand the need of `import ScalaDerivation._` without this
    implicitly[Decoder[Length]]   // IntelliJ doesn't understand the need of `import LengthSerializer._` without this

    private[distances] implicit final val encoder: Encoder[Distance] = deriveEncoder[Distance]
    private[distances] implicit final val decoder: Decoder[Distance] = deriveDecoder[Distance]
  }

  final case class DirectedPath(origin: LatLong, destination: LatLong, travelModes: List[TravelMode])

  sealed trait TravelMode extends EnumEntry
  object TravelMode extends Enum[TravelMode] {

    val values: immutable.IndexedSeq[TravelMode] = findValues

    case object Driving   extends TravelMode
    case object Bicycling extends TravelMode
    case object Walking   extends TravelMode
    case object Transit   extends TravelMode
    case object Unknown   extends TravelMode
  }

}
