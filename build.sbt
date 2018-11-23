import sbt.Keys.crossScalaVersions

lazy val scala212 = "2.12.7"
lazy val scala211 = "2.11.12"

ThisBuild / organization := "com.guizmaii"
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala211, scala212)
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true

//// Dependencies

val scalaCacheVersion = "0.26.0"
val circeVersion      = "0.10.1"

lazy val googleMaps      = "com.google.maps"   % "google-maps-services" % "0.9.0"
lazy val squants         = "org.typelevel"     %% "squants"             % "1.4.0"
lazy val cats            = "org.typelevel"     %% "cats-core"           % "1.4.0"
lazy val `cats-effect`   = "org.typelevel"     %% "cats-effect"         % "1.0.0"
lazy val `cats-par`      = "io.chrisdavenport" %% "cats-par"            % "0.2.0"
lazy val enumeratum      = "com.beachape"      %% "enumeratum"          % "1.5.13"
lazy val `circe-generic` = "io.circe"          %% "circe-generic"       % circeVersion
lazy val monix           = "io.monix"          %% "monix"               % "3.0.0-RC2"

lazy val scalacacheCore =
  Seq(
    "com.github.cb372" %% "scalacache-core"        % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-cats-effect" % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-circe"       % scalaCacheVersion,
  )

lazy val testKit = {
  val kantancsv = ((version: String) =>
    Seq(
      "com.nrinaudo" %% "kantan.csv"         % version,
      "com.nrinaudo" %% "kantan.csv-cats"    % version,
      "com.nrinaudo" %% "kantan.csv-generic" % version
    ))("0.5.0")

  Seq(
    "org.scalacheck" %% "scalacheck"            % "1.14.0",
    "org.scalatest"  %% "scalatest"             % "3.0.5",
    "com.beachape"   %% "enumeratum-scalacheck" % "1.5.15",
    "io.circe"       %% "circe-literal"         % circeVersion,
    monix
  ) ++ kantancsv
}.map(_ % Test)

//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`, tests, benchmarks)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`, tests, benchmarks)

lazy val core = project
  .settings(moduleName := "scala-distances-core")
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(
    libraryDependencies ++= Seq(
      squants,
      cats,
      `cats-effect`,
      `cats-par`,
      enumeratum,
      `circe-generic`
    ) ++ scalacacheCore ++ testKit)

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-provider-google")
  .settings(libraryDependencies += googleMaps)
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-cache-redis")
  .settings(libraryDependencies += "com.github.cb372" %% "scalacache-redis" % scalaCacheVersion)
  .dependsOn(core)

lazy val `caffeine-cache` = project
  .in(file("caches/caffeine"))
  .settings(moduleName := "scala-distances-cache-caffeine")
  .settings(libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion)
  .dependsOn(core)

lazy val `no-cache` = project
  .in(file("caches/no-cache"))
  .settings(moduleName := "scala-distances-cache-noCache")
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= testKit)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`)

lazy val benchmarks = project
  .enablePlugins(JmhPlugin)
  .settings(noPublishSettings)
  .settings(libraryDependencies += monix)
  .dependsOn(core)

//// Publishing settings

/**
  * Copied from Cats
  */
lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

inThisBuild(
  List(
    credentials += Credentials(Path.userHome / ".bintray" / ".credentials"),
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    scmInfo := Some(ScmInfo(url("https://github.com/guizmaii/scala-distances"), "scm:git:git@github.com:guizmaii/scala-distances.git")),
    homepage := Some(url("https://github.com/guizmaii/scala-distances")),
    developers := List(Developer("guizmaii", "Jules Ivanic", "jules.ivanic@gmail.com", url("https://guizmaii.github.io/"))),
    bintrayOrganization := Some("guizmaii"),
    bintrayReleaseOnPublish := true,
    publishMavenStyle := true,
    releaseCrossBuild := true
  )
)

//// Aliases

/**
  * Copied from kantan.csv
  */
addCommandAlias("runBench", "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1")
