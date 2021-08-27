package com.colisweb.distances.model

sealed trait TravelMode {
  def maxSpeed: Option[SpeedInKmH]
  def copy(newSpeed: SpeedInKmH): TravelMode
}

object TravelMode {

  case class Car(maxSpeed: Option[SpeedInKmH] = None) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Car(Some(newSpeed))
  }
  case class Truck(
      maxSpeed: Option[SpeedInKmH] = None,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  ) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Truck(Some(newSpeed), weight, length, width, height)
  }
  case class Scooter(maxSpeed: Option[SpeedInKmH] = None) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Scooter(Some(newSpeed))
  }
  case class Pedestrian(maxSpeed: Option[SpeedInKmH] = None) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Pedestrian(Some(newSpeed))

  }
  case class Bicycle(maxSpeed: Option[SpeedInKmH] = None) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Bicycle(Some(newSpeed))
  }

}
