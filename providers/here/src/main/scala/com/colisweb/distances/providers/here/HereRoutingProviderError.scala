package com.colisweb.distances.providers.here

import com.colisweb.distances.model.DistanceError

import scala.util.control.NoStackTrace

sealed abstract class HereRoutingProviderError(message: String)
    extends RuntimeException(message)
    with DistanceError
    with NoStackTrace

final case class UnauthorizedRequest(message: String) extends HereRoutingProviderError(message)
final case class MalformedRequest(message: String)    extends HereRoutingProviderError(message)
final case class NonAllowedRequest(message: String)   extends HereRoutingProviderError(message)
final case class UnknownHereError(message: String)    extends HereRoutingProviderError(message)
final case class UnknownHereResponse(message: String) extends HereRoutingProviderError(message)

final case object TooManyRequestError
    extends HereRoutingProviderError(s"You reached the maximum RPS for your current plan")
