package com.colisweb.distances.providers.here

import com.colisweb.distances.model.TravelMode

object HereAdaptor {

  implicit final class HereTravelModeOps(mode: TravelMode) {
    def asHere: Map[String, String] =
      mode match {
        case mode: TravelMode.Car =>
          Map("transportMode" -> "car", "vehicle[speedCap]" -> s"${mode.maxSpeed / 3.6}")
        case mode: TravelMode.Truck =>
          val weight = mode.weight.map(w => "truck[grossWeight]" -> w.toString)
          val height = mode.height.map(h => "truck[height]" -> h.toString)
          val length = mode.length.map(l => "truck[length]" -> l.toString)
          val width  = mode.width.map(w => "truck[width]" -> w.toString)
          Map(
            "transportMode"     -> "truck",
            "vehicle[speedCap]" -> s"${mode.maxSpeed / 3.6}"
          ) ++ weight ++ height ++ width ++ length

        case mode: TravelMode.Scooter =>
          Map(
            "transportMode"     -> "scooter",
            "vehicle[speedCap]" -> s"${mode.maxSpeed / 3.6}"
          )
        case mode: TravelMode.Pedestrian =>
          Map(
            "transportMode"     -> "truck",
            "pedestrian[speed]" -> s"${mode.maxSpeed / 3.6}"
          )
        case _: TravelMode.Bicycle =>
          Map("transportMode" -> "bicycle")
      }
  }
}
