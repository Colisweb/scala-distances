package com.colisweb.distances.providers.here

import com.colisweb.distances.model.TravelMode

object HereAdaptor {

  implicit final class HereTravelModeOps(mode: TravelMode) {
    def asHere: String =
      mode match {
        case TravelMode.Car        => "car"
        case TravelMode.Truck      => "truck"
        case TravelMode.Scooter    => "scooter"
        case TravelMode.Pedestrian => "pedestrian"
        case TravelMode.Bicycle    => "bicycle"
      }
  }
}
