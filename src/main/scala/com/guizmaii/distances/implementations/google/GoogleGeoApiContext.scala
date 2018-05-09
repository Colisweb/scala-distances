package com.guizmaii.distances.implementations.google

import java.util.concurrent.TimeUnit

import com.google.maps.GeoApiContext

import scala.concurrent.duration._
import scala.language.postfixOps

final case class GoogleGeoApiContext(googleApiKey: String, connectTimeout: Duration, readTimeout: Duration) {

  /**
    * More infos about the rate limit:
    *   - https://developers.google.com/maps/documentation/distance-matrix/usage-limits
    */
  val geoApiContext: GeoApiContext =
    new GeoApiContext.Builder()
      .apiKey(googleApiKey)
      .connectTimeout(connectTimeout.toMillis, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toMillis, TimeUnit.MILLISECONDS)
      .queryRateLimit(100)
      .build()

}

object GoogleGeoApiContext {
  def apply(googleApiKey: String): GoogleGeoApiContext = new GoogleGeoApiContext(googleApiKey, 1 second, 1 second)
}
