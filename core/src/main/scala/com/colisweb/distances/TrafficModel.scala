package com.colisweb.distances

import cats.Show
import com.google.maps.model.TrafficModel.{BEST_GUESS, OPTIMISTIC, PESSIMISTIC}
import com.google.maps.model.{TrafficModel => GoogleTrafficModel}
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TrafficModel extends EnumEntry
object TrafficModel extends Enum[TravelMode] {
  val values: immutable.IndexedSeq[TravelMode] = findValues

  case object BestGuess   extends TrafficModel
  case object Optimistic  extends TrafficModel
  case object Pessimistic extends TrafficModel

  implicit final val show: Show[TrafficModel] = new Show[TrafficModel] {
    override def show(trafficModel: TrafficModel): String =
      trafficModel match {
        case BestGuess   => "best_guess"
        case Optimistic  => "optimistic"
        case Pessimistic => "pessimistic"
      }
  }

  implicit final class ToGoogleType(model: TrafficModel) {
    def asGoogle: GoogleTrafficModel = model match {
      case BestGuess   => BEST_GUESS
      case Optimistic  => OPTIMISTIC
      case Pessimistic => PESSIMISTIC
    }
  }
}
