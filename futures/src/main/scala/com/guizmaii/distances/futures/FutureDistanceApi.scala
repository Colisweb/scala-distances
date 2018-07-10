package com.guizmaii.distances.futures

import com.guizmaii.distances.{Cache, DistanceApi, DistanceProvider}

import scala.concurrent.Future

class FutureDistanceApi(distanceProvider: DistanceProvider[Future], cache: Cache[Future])
    extends DistanceApi[Future](distanceProvider, cache)
