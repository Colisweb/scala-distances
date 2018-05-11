package com.guizmaii.distances.utils.circe

import java.time.temporal.ChronoUnit
import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit

import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.duration.Duration

object ScalaDerivation {

  import io.circe.java8.time.{decodeDuration => decodeJavaDuration, encodeDuration => encodeJavaDuration}

  private[this] final val asJavaDuration: Duration => JavaDuration  = d => JavaDuration.of(d.toNanos.longValue(), ChronoUnit.NANOS)
  private[this] final val asScalaDuration: JavaDuration => Duration = d => Duration.apply(d.getNano.longValue(), TimeUnit.NANOSECONDS)

  /**
    * TODO: Could be implemented without passing by java.time.Duration ?
    */
  implicit final val durationEncoder: Encoder[Duration] = new Encoder[Duration] {
    override def apply(a: Duration): Json = (asJavaDuration andThen encodeJavaDuration.apply)(a)
  }

  /**
    * TODO: Could be implemented without passing by java.time.Duration ?
    */
  implicit final val durationDecoder: Decoder[Duration] = decodeJavaDuration.map(asScalaDuration)

}
