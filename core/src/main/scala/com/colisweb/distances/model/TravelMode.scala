package com.colisweb.distances.model

sealed trait TravelMode { def maxSpeed: SpeedInKmH }
object TravelMode {
  case class Car(maxSpeed: SpeedInKmH) extends TravelMode
  case class Truck(
      maxSpeed: SpeedInKmH,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  )                                           extends TravelMode
  case class Scooter(maxSpeed: SpeedInKmH)    extends TravelMode
  case class Pedestrian(maxSpeed: SpeedInKmH) extends TravelMode
  case class Bicycle(maxSpeed: SpeedInKmH)    extends TravelMode
}
