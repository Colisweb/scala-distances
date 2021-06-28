package com.colisweb.distances.providers.google

import com.colisweb.distances.model.{Point, TravelMode}
import com.colisweb.distances.providers.google.TrafficModel.{BestGuess, Optimistic, Pessimistic}
import com.google.maps.model.TrafficModel.{BEST_GUESS, OPTIMISTIC, PESSIMISTIC}
import com.google.maps.model.TravelMode._
import com.google.maps.model.{
  LatLng => GoogleLatLong,
  TrafficModel => GoogleTrafficModel,
  TravelMode => GoogleTravelMode
}

object GoogleModel {

  implicit final class GoogleTrafficModelOps(model: TrafficModel) {
    def asGoogle: GoogleTrafficModel =
      model match {
        case BestGuess   => BEST_GUESS
        case Optimistic  => OPTIMISTIC
        case Pessimistic => PESSIMISTIC
      }
  }

  implicit final class GoogleTravelModeOps(mode: TravelMode) {
    def asGoogle: GoogleTravelMode =
      mode match {
        case _: TravelMode.Car        => DRIVING
        case _: TravelMode.Truck      => DRIVING
        case _: TravelMode.Scooter    => DRIVING
        case _: TravelMode.Pedestrian => WALKING
        case _: TravelMode.Bicycle    => BICYCLING
      }
  }

  implicit final class GoogleLatLongOps(point: Point) {
    def asGoogle: GoogleLatLong = new GoogleLatLong(point.latitude, point.longitude)
  }
}
