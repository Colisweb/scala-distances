package com.colisweb.distances.providers.here

import com.colisweb.distances.model.{DistanceError, Point}

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
final case class NoRouteFoundError(origin: Point, destination: Point)
    extends HereRoutingProviderError(s"Here found no route between $origin and $destination")

final case object TooManyRequestError
    extends HereRoutingProviderError("You reached the maximum RPS for your current plan")
