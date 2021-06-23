package com.colisweb.distances.model

sealed trait TravelMode {
  def maxSpeed: SpeedInKmH
  def copy(newSpeed: SpeedInKmH): TravelMode
}

object TravelMode {

  case class Car(maxSpeed: SpeedInKmH = 130d) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Car(newSpeed)
  }
  case class Truck(
      maxSpeed: SpeedInKmH = 110d,
      weight: Option[WeightInKg] = None,
      length: Option[DimensionInCm] = None,
      width: Option[DimensionInCm] = None,
      height: Option[DimensionInCm] = None
  ) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Truck(newSpeed, weight, length, width, height)
  }
  case class Scooter(maxSpeed: SpeedInKmH = 110d) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Scooter(newSpeed)
  }
  case class Pedestrian(maxSpeed: SpeedInKmH = 10d) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Pedestrian(newSpeed)

  }
  case class Bicycle(maxSpeed: SpeedInKmH = 30d) extends TravelMode {
    override def copy(newSpeed: SpeedInKmH): TravelMode = Bicycle(newSpeed)
  }

}
