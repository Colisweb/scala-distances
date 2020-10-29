package com.colisweb.distances.providers.google

import java.time.Instant

import com.colisweb.distances.model.DistanceError

import scala.util.control.NoStackTrace

sealed abstract class GoogleDistanceProviderError(message: String)
    extends RuntimeException(message)
    with DistanceError
    with NoStackTrace

final case class DistanceNotFound(message: String)   extends GoogleDistanceProviderError(message)
final case class NoResults(message: String)          extends GoogleDistanceProviderError(message)
final case class UnknownGoogleError(message: String) extends GoogleDistanceProviderError(message)

final case class PastTraffic(departureTime: Instant)
    extends GoogleDistanceProviderError(s"Google does not handle traffic in the past (departureTime: $departureTime)")
