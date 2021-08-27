package com.colisweb.distances.providers.here

import com.colisweb.distances.model.TravelMode

object HereAdaptor {

  implicit final class HereTravelModeOps(mode: TravelMode) {
    def asHere: Map[String, String] =
      mode match {
        case mode: TravelMode.Car =>
          val speed = mode.maxSpeed.map(s => "vehicle[speedCap]" -> s"${s / 3.6}")
          Map("transportMode" -> "car") ++ speed

        case mode: TravelMode.Truck =>
          val weight = mode.weight.map(w => "truck[grossWeight]" -> w.toString)
          val height = mode.height.map(h => "truck[height]" -> h.toString)
          val length = mode.length.map(l => "truck[length]" -> l.toString)
          val width  = mode.width.map(w => "truck[width]" -> w.toString)
          val speed  = mode.maxSpeed.map(s => "vehicle[speedCap]" -> s"${s / 3.6}")
          Map("transportMode" -> "truck") ++ weight ++ height ++ width ++ length ++ speed

        case mode: TravelMode.Scooter =>
          val speed = mode.maxSpeed.map(s => "vehicle[speedCap]" -> s"${s / 3.6}")
          Map("transportMode" -> "scooter") ++ speed

        case mode: TravelMode.Pedestrian =>
          val speed = mode.maxSpeed.map(s => "pedestrian[speed]" -> s"${s / 3.6}")
          Map("transportMode" -> "pedestrian") ++ speed

        case _: TravelMode.Bicycle =>
          Map("transportMode" -> "bicycle")
      }
  }
}
