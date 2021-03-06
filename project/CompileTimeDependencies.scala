import sbt._

object Versions {
  lazy val catsEffect         = "2.3.3"
  lazy val circe              = "0.13.0"
  lazy val circeOptics        = "0.13.0"
  lazy val enumeratum         = "1.6.1"
  lazy val google             = "0.17.0"
  final val logbackEncoder    = "6.6"
  lazy val loggingInterceptor = "4.9.1"
  lazy val monix              = "3.3.0"
  lazy val pureconfig         = "0.15.0"
  lazy val refined            = "0.9.23"
  lazy val requests           = "0.6.9"
  lazy val scalaCache         = "0.28.0"
  lazy val scalaCompat        = "2.4.2"
  lazy val squants            = "1.7.0"
}

object CompileTimeDependencies {
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  lazy val circe              = "io.circe" %% "circe-core"           % Versions.circe
  lazy val circeGeneric       = "io.circe" %% "circe-generic"        % Versions.circe
  lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % Versions.circe
  lazy val circeOptics        = "io.circe" %% "circe-optics"         % Versions.circeOptics
  lazy val circeParser        = "io.circe" %% "circe-parser"         % Versions.circe
  lazy val circeRefined       = "io.circe" %% "circe-refined"        % Versions.circe

  lazy val enumeratum            = "com.beachape"        %% "enumeratum"               % Versions.enumeratum
  lazy val googleMaps            = "com.google.maps"      % "google-maps-services"     % Versions.google
  lazy val loggingInterceptor    = "com.squareup.okhttp3" % "logging-interceptor"      % Versions.loggingInterceptor
  lazy val logstashLogbackEncode = "net.logstash.logback" % "logstash-logback-encoder" % Versions.logbackEncoder

  lazy val monix             = "io.monix"              %% "monix"              % Versions.monix
  lazy val pureconfig        = "com.github.pureconfig" %% "pureconfig"         % Versions.pureconfig
  lazy val refinedPureconfig = "eu.timepit"            %% "refined-pureconfig" % Versions.refined

  lazy val requests = "com.lihaoyi" %% "requests" % Versions.requests

  lazy val scalaCache           = "com.github.cb372"       %% "scalacache-core"         % Versions.scalaCache
  lazy val scalaCacheCaffeine   = "com.github.cb372"       %% "scalacache-caffeine"     % Versions.scalaCache
  lazy val scalaCacheCatsEffect = "com.github.cb372"       %% "scalacache-cats-effect"  % Versions.scalaCache
  lazy val scalaCacheCirce      = "com.github.cb372"       %% "scalacache-circe"        % Versions.scalaCache
  lazy val scalaCacheRedis      = "com.github.cb372"       %% "scalacache-redis"        % Versions.scalaCache
  lazy val scalaCompat          = "org.scala-lang.modules" %% "scala-collection-compat" % Versions.scalaCompat

  lazy val squants = "org.typelevel" %% "squants" % Versions.squants

}
