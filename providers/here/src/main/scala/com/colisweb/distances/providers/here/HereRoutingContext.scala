package com.colisweb.distances.providers.here

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

final case class HereRoutingContext(
    apiKey: String,
    connectTimeout: Duration,
    readTimeout: Duration
) {
  assert(apiKey.trim.nonEmpty, "apiKey must be a non empty String")

}

object HereRoutingContext {
  final def apply(hereApiKey: String): HereRoutingContext =
    new HereRoutingContext(hereApiKey, 1 second, 1 second)
}
