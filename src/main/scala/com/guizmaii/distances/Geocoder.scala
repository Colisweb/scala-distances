package com.guizmaii.distances

import com.guizmaii.distances.Types.{LatLong, PostalCode}
import com.guizmaii.distances.utils.WithCache
import monix.eval.Task
import monix.execution.CancelableFuture

trait Geocoder extends WithCache[LatLong] {
  def geocodeT(postalCode: PostalCode): Task[LatLong]
  def geocode(postalCode: PostalCode): CancelableFuture[LatLong]
}
