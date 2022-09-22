package com.colisweb.distances.providers.here

import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration._
import scala.language.postfixOps

final case class HereRoutingContext(
    apiKey: NonEmptyString,
    connectTimeout: Duration,
    readTimeout: Duration
)

object HereRoutingContext {
  final def apply(hereApiKey: NonEmptyString): HereRoutingContext =
    new HereRoutingContext(hereApiKey, 1 second, 1 second)
}
