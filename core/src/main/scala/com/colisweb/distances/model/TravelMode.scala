package com.colisweb.distances.model

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TravelMode extends EnumEntry { def maxSpeed: SpeedInKmH }

object TravelMode extends Enum[TravelMode] {
  val values: immutable.IndexedSeq[TravelMode] = findValues
  private val defaultMaxSpeed: SpeedInKmH      = 250d

  case class Car(maxSpeed: SpeedInKmH = defaultMaxSpeed) extends TravelMode
  case class Truck(
      maxSpeed: SpeedInKmH = defaultMaxSpeed,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  )                                                             extends TravelMode
  case class Scooter(maxSpeed: SpeedInKmH = defaultMaxSpeed)    extends TravelMode
  case class Pedestrian(maxSpeed: SpeedInKmH = defaultMaxSpeed) extends TravelMode
  case class Bicycle(maxSpeed: SpeedInKmH = defaultMaxSpeed)    extends TravelMode

}
