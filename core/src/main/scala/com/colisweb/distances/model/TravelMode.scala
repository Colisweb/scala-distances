package com.colisweb.distances.model

sealed trait TravelMode
object TravelMode {
  case object Car        extends TravelMode
  case object Truck      extends TravelMode
  case object Scooter    extends TravelMode
  case object Pedestrian extends TravelMode
  case object Bicycle    extends TravelMode
}
