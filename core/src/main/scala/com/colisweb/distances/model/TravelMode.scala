package com.colisweb.distances.model

sealed trait TravelMode { def maxSpeed: SpeedInKmH }
object TravelMode {
  case class Car(maxSpeed: SpeedInKmH = 250) extends TravelMode
  case class Truck(
      maxSpeed: SpeedInKmH = 250,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  )                                                 extends TravelMode
  case class Scooter(maxSpeed: SpeedInKmH = 250)    extends TravelMode
  case class Pedestrian(maxSpeed: SpeedInKmH = 250) extends TravelMode
  case class Bicycle(maxSpeed: SpeedInKmH = 250)    extends TravelMode
}
