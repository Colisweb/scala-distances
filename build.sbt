import sbt.Keys.{crossScalaVersions, moduleName, scalacOptions}

organization in ThisBuild := "com.guizmaii"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalafmtOnCompile in ThisBuild := true
scalafmtCheck in ThisBuild := true
scalafmtSbtCheck in ThisBuild := true

/* Dependencies */

lazy val monix         = "io.monix"        %% "monix"               % "3.0.0-RC1"
lazy val googleMaps    = "com.google.maps" % "google-maps-services" % "0.2.7"
lazy val squants       = "org.typelevel"   %% "squants"             % "1.3.0"
lazy val cats          = "org.typelevel"   %% "cats-core"           % "1.1.0"
lazy val `cats-effect` = "org.typelevel"   %% "cats-effect"         % "1.0.0-RC"
lazy val enumeratum    = "com.beachape"    %% "enumeratum"          % "1.5.13"

lazy val kantancsv = ((version: String) =>
  Seq(
    "com.nrinaudo" %% "kantan.csv"         % version,
    "com.nrinaudo" %% "kantan.csv-cats"    % version,
    "com.nrinaudo" %% "kantan.csv-generic" % version
  ))("0.4.0")

lazy val scalacache = ((version: String) =>
  Seq(
    "com.github.cb372" %% "scalacache-core"     % version,
    "com.github.cb372" %% "scalacache-caffeine" % version,
    "com.github.cb372" %% "scalacache-redis"    % version,
    "com.github.cb372" %% "scalacache-monix"    % version
  ))("0.24.1")

lazy val testKit = Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "org.scalatest"  %% "scalatest"  % "3.0.5"
) ++ kantancsv

lazy val coreDependencies = Seq(
  monix   % Provided,
  squants % Provided,
  `cats-effect`,
  cats,
  enumeratum,
  googleMaps
) ++ scalacache ++ testKit.map(_ % Test)

lazy val scalaDistancesSettings = Seq(
  scalaVersion := scala212,
  crossScalaVersions := Seq(scala211, scala212)
)

/* Modules */

lazy val `scala-distances` = project
  .in(file("."))
  .settings(moduleName := "scala-distances")
  .settings(scalaDistancesSettings)
  .settings(noPublishSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val core = project
  .in(file("core"))
  .settings(moduleName := "scala-distance-core")
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(scalaDistancesSettings)
  .settings(libraryDependencies ++= coreDependencies)

/* Publishing configurations */

// Copied from Cats
lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

inThisBuild(
  List(
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    scmInfo := Some(ScmInfo(url("https://github.com/guizmaii/scala-distances"), "scm:git:git@github.com:guizmaii/scala-distances.git")),
    homepage := Some(url("https://github.com/guizmaii/scala-distances")),
    developers := List(Developer("guizmaii", "Jules Ivanic", "jules.ivanic@gmail.com", url("https://guizmaii.github.io/"))),
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher
  )
)
