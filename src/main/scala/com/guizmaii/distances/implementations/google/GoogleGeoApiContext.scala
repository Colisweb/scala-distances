package com.guizmaii.distances.implementations.google

import com.google.maps.GeoApiContext

final case class GoogleGeoApiContext(googleApiKey: String) {

  /**
    * More infos about the rate limit:
    *   - https://developers.google.com/maps/documentation/distance-matrix/usage-limits
    */
  val geoApiContext: GeoApiContext = new GeoApiContext.Builder().apiKey(googleApiKey).queryRateLimit(100).build()

}
