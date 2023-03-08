import sbt._

object Versions {
  final val catsEffect         = "2.3.3"
  final val circe              = "0.14.2"
  final val circeOptics        = "0.14.1"
  final val enumeratum         = "1.7.0"
  final val google             = "2.0.0"
  final val logbackEncoder     = "7.2"
  final val loggingInterceptor = "4.10.0"
  final val monix              = "3.4.1"
  final val pureconfig         = "0.17.1"
  final val refined            = "0.10.1"
  final val requests           = "0.7.1"
  final val scalaCache         = "0.28.0"
  final val scalaCompat        = "2.8.1"
  final val squants            = "1.8.3"

  final val mockitoScala  = "1.17.12"
  final val kantan        = "0.7.0"
  final val scalacheck    = "1.17.0"
  final val scalatest     = "3.2.13"
  final val scalatestPlus = "3.1.0.0-RC2"
}

object Dependencies {
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

  final val mockitoScalaScalatest = "org.mockito"       %% "mockito-scala-scalatest"  % Versions.mockitoScala
  final val kantan                = "com.nrinaudo"      %% "kantan.csv"               % Versions.kantan
  final val kantanCats            = "com.nrinaudo"      %% "kantan.csv-cats"          % Versions.kantan
  final val kantanGeneric         = "com.nrinaudo"      %% "kantan.csv-generic"       % Versions.kantan
  final val circeLiteral          = "io.circe"          %% "circe-literal"            % Versions.circe
  final val scalacheck            = "org.scalacheck"    %% "scalacheck"               % Versions.scalacheck
  final val scalatestPlus         = "org.scalatestplus" %% "scalatestplus-scalacheck" % Versions.scalatestPlus
  final val scalatest             = "org.scalatest"     %% "scalatest"                % Versions.scalatest
  final val enumeratumScalacheck  = "com.beachape"      %% "enumeratum-scalacheck"    % Versions.enumeratum

}