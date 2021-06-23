package com.colisweb.distances.model

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TravelMode extends EnumEntry {
  def maxSpeed: SpeedInKmH
  def copy(newSpeed: SpeedInKmH): TravelMode
}

object TravelMode extends Enum[TravelMode] {
  val values: immutable.IndexedSeq[TravelMode] = findValues

  case class Car(maxSpeed: SpeedInKmH = 130d) extends TravelMode {
    override val entryName: String                      = "car"

    override def copy(newSpeed: SpeedInKmH): TravelMode = Car(newSpeed)
  }
  case class Truck(
      maxSpeed: SpeedInKmH = 110d,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  ) extends TravelMode {
    override val entryName: String                      = "truck"

    override def copy(newSpeed: SpeedInKmH): TravelMode = Truck(newSpeed, weight, length, width, height)
  }
  case class Scooter(maxSpeed: SpeedInKmH = 110d) extends TravelMode {
    override val entryName: String                      = "scooter"

    override def copy(newSpeed: SpeedInKmH): TravelMode = Scooter(newSpeed)
  }
  case class Pedestrian(maxSpeed: SpeedInKmH = 10d) extends TravelMode {
    override val entryName: String                      = "pedestrian"

    override def copy(newSpeed: SpeedInKmH): TravelMode = Pedestrian(newSpeed)

  }
  case class Bicycle(maxSpeed: SpeedInKmH = 30d) extends TravelMode {
    override val entryName: String                      = "bicycle"

    override def copy(newSpeed: SpeedInKmH): TravelMode = Bicycle(newSpeed)
  }

}
