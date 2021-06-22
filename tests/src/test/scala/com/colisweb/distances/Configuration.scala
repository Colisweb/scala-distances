package com.colisweb.distances

import com.colisweb.distances.Configuration.{Google, Here, Redis}
import com.colisweb.distances.caches.RedisConfiguration
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.generic.auto._
import pureconfig.{ConfigConvert, ConfigReader, ConfigSource}

final case class Configuration(google: Google, here: Here, redis: Redis)

object Configuration {

  def load: Configuration = {
    implicitly[ConfigConvert[NonEmptyString]]
    implicitly[ConfigReader[Google]]
    implicitly[ConfigReader[Redis]]
    ConfigSource.default.loadOrThrow[Configuration]
  }

  final case class Google(apiKey: NonEmptyString)

  final case class Here(apiKey: NonEmptyString)

  final case class Redis(host: String, port: Int) {
    def asConfiguration: RedisConfiguration = RedisConfiguration(host, port)
  }
}
