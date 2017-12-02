package com.guizmaii.distances

import com.guizmaii.distances.Types._
import com.guizmaii.distances.utils.WithCache
import monix.eval.Task
import monix.execution.CancelableFuture

import scala.collection.immutable.Seq

trait DistanceApi extends WithCache[SerializableDistance] {

  def distanceT(origin: LatLong, destination: LatLong): Task[Distance]

  def distance(origin: LatLong, destination: LatLong): CancelableFuture[Distance]

  def distanceFromPostalCodesT(geocoder: Geocoder)(origin: PostalCode, destination: PostalCode): Task[Distance]

  def distanceFromPostalCodes(geocoder: Geocoder)(
      origin: PostalCode,
      destination: PostalCode
  ): CancelableFuture[Distance]

  def distancesT(paths: Seq[DirectedPath]): Task[Seq[DirectedPathWithDistance]]

  def distances(paths: Seq[DirectedPath]): CancelableFuture[Seq[DirectedPathWithDistance]]

}
