import sbt._

object Versions {
  final val catsEffect         = "2.3.3"
  final val circe              = "0.14.2"
  final val circeOptics        = "0.14.2"
  final val enumeratum         = "1.6.1"
  final val google             = "0.17.0"
  final val logbackEncoder     = "7.0.1"
  final val loggingInterceptor = "4.9.1"
  final val monix              = "3.3.0"
  final val pureconfig         = "0.17.1"
  final val refined            = "0.9.23"
  final val requests           = "0.7.0"
  final val scalaCache         = "0.28.0"
  final val scalaCompat        = "2.4.2"
  final val squants            = "1.7.0"
}

object CompileTimeDependencies {
  final val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  final val circe              = "io.circe" %% "circe-core"           % Versions.circe
  final val circeGeneric       = "io.circe" %% "circe-generic"        % Versions.circe
  final val circeGenericExtras = "io.circe" %% "circe-generic-extras" % Versions.circe
  final val circeOptics        = "io.circe" %% "circe-optics"         % Versions.circeOptics
  final val circeParser        = "io.circe" %% "circe-parser"         % Versions.circe
  final val circeRefined       = "io.circe" %% "circe-refined"        % Versions.circe

  final val enumeratum            = "com.beachape"        %% "enumeratum"               % Versions.enumeratum
  final val googleMaps            = "com.google.maps"      % "google-maps-services"     % Versions.google
  final val loggingInterceptor    = "com.squareup.okhttp3" % "logging-interceptor"      % Versions.loggingInterceptor
  final val logstashLogbackEncode = "net.logstash.logback" % "logstash-logback-encoder" % Versions.logbackEncoder

  final val monix             = "io.monix"              %% "monix"              % Versions.monix
  final val pureconfig        = "com.github.pureconfig" %% "pureconfig"         % Versions.pureconfig
  final val refinedPureconfig = "eu.timepit"            %% "refined-pureconfig" % Versions.refined

  final val requests = "com.lihaoyi" %% "requests" % Versions.requests

  final val scalaCache           = "com.github.cb372"       %% "scalacache-core"         % Versions.scalaCache
  final val scalaCacheCaffeine   = "com.github.cb372"       %% "scalacache-caffeine"     % Versions.scalaCache
  final val scalaCacheCatsEffect = "com.github.cb372"       %% "scalacache-cats-effect"  % Versions.scalaCache
  final val scalaCacheCirce      = "com.github.cb372"       %% "scalacache-circe"        % Versions.scalaCache
  final val scalaCacheRedis      = "com.github.cb372"       %% "scalacache-redis"        % Versions.scalaCache
  final val scalaCompat          = "org.scala-lang.modules" %% "scala-collection-compat" % Versions.scalaCompat

  final val squants = "org.typelevel" %% "squants" % Versions.squants

}
