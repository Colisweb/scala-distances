import sbt.*

object Versions {
  final val approvals          = "1.3.1"
  final val circe              = "0.14.2"
  final val enumeratum         = "1.7.3"
  final val google             = "2.2.0"
  final val logbackEncoder     = "7.4"
  final val loggingInterceptor = "4.11.0"
  final val mockitoScala       = "1.17.22"
  final val pureconfig         = "0.17.4"
  final val refined            = "0.11.0"
  final val requests           = "0.7.1"
  final val scalacheck         = "1.17.0"
  final val scalaCompat        = "2.11.0"
  final val scalatest          = "3.2.16"
  final val scalatestPlus      = "3.1.0.0-RC2"
  final val squants            = "1.8.3"
  final val simplecache        = "2.1.1"
}

object Dependencies {

  final val approvals             = "com.colisweb"           %% "approvals-scala"          % Versions.approvals
  final val circe                 = "io.circe"               %% "circe-core"               % Versions.circe
  final val circeGeneric          = "io.circe"               %% "circe-generic"            % Versions.circe
  final val circeGenericExtras    = "io.circe"               %% "circe-generic-extras"     % Versions.circe
  final val circeJawn             = "io.circe"               %% "circe-jawn"               % Versions.circe
  final val enumeratum            = "com.beachape"           %% "enumeratum"               % Versions.enumeratum
  final val googleMaps            = "com.google.maps"         % "google-maps-services"     % Versions.google
  final val loggingInterceptor    = "com.squareup.okhttp3"    % "logging-interceptor"      % Versions.loggingInterceptor
  final val logstashLogbackEncode = "net.logstash.logback"    % "logstash-logback-encoder" % Versions.logbackEncoder
  final val mockitoScalaScalatest = "org.mockito"            %% "mockito-scala-scalatest"  % Versions.mockitoScala
  final val pureconfig            = "com.github.pureconfig"  %% "pureconfig"               % Versions.pureconfig
  final val refined               = "eu.timepit"             %% "refined"                  % Versions.refined
  final val refinedPureconfig     = "eu.timepit"             %% "refined-pureconfig"       % Versions.refined
  final val requests              = "com.lihaoyi"            %% "requests"                 % Versions.requests
  final val scalacheck            = "org.scalacheck"         %% "scalacheck"               % Versions.scalacheck
  final val scalaCompat           = "org.scala-lang.modules" %% "scala-collection-compat"  % Versions.scalaCompat
  final val scalatest             = "org.scalatest"          %% "scalatest"                % Versions.scalatest
  final val scalatestPlus         = "org.scalatestplus"      %% "scalatestplus-scalacheck" % Versions.scalatestPlus
  final val squants               = "org.typelevel"          %% "squants"                  % Versions.squants
  final val simplecacheWrapperCats = "com.colisweb" %% "simplecache-wrapper-cats" % Versions.simplecache
  final val simplecacheRedisCirce  = "com.colisweb" %% "simplecache-redis-circe"  % Versions.simplecache
  final val simplecacheMemoryGuava = "com.colisweb" %% "simplecache-memory-guava" % Versions.simplecache
  final val simplecacheMemory      = "com.colisweb" %% "simplecache-memory"       % Versions.simplecache

}
