package com.colisweb.distances.providers.here

import cats.MonadError
import cats.effect.Concurrent
import requests.Response

trait RequestExecutor[F[_]] {
  def run(request: Response): F[Response]
}

class SyncRequestExecutor[F[_]](implicit F: MonadError[F, Throwable]) extends RequestExecutor[F] {
  override def run(request: Response): F[Response] = F.catchNonFatal(request)
}

class AsyncRequestExecutor[F[_]](implicit F: Concurrent[F]) extends RequestExecutor[F] {
  override def run(request: Response): F[Response] = F.delay(request)
}
