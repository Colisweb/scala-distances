package com.colisweb.distances.providers.google

import java.util.concurrent.TimeUnit

import com.google.maps.GeoApiContext

import scala.concurrent.duration._
import scala.language.postfixOps

final case class GoogleGeoApiContext(apiKey: String, connectTimeout: Duration, readTimeout: Duration, queryRateLimit: Int) {

  /**
    * More infos about the rate limit:
    *   - https://developers.google.com/maps/documentation/distance-matrix/usage-and-billing
    *
    * Useful information taken from this documentation at the date of 2018.11.23:
    *
    * ```
    * Other Usage Limits
    * ------------------
    * While you are no longer limited to a maximum number of elements per day (EPD), the following usage limits are still in place for the Distance Matrix API:
    *
    *  - Maximum of 25 origins or 25 destinations per request.
    *  - Maximum 100 elements that include Departure Time, per request.
    *  - Maximum 100 elements per client-side request.
    *  - 1000 elements per second (EPS), calculated as the sum of client-side and server-side queries.
    * ```
    */
  final val geoApiContext: GeoApiContext =
    new GeoApiContext.Builder()
      .apiKey(apiKey)
      .connectTimeout(connectTimeout.toMillis, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toMillis, TimeUnit.MILLISECONDS)
      .queryRateLimit(queryRateLimit)
      .build()

}

object GoogleGeoApiContext {
  final def apply(googleApiKey: String): GoogleGeoApiContext = new GoogleGeoApiContext(googleApiKey, 1 second, 1 second, 1000)
}
