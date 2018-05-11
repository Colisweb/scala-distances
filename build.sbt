import sbt.Keys.crossScalaVersions

organization := "com.guizmaii"

name := "scala-distances"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalaVersion := scala212
crossScalaVersions := Seq(scala211, scala212)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

scalafmtOnCompile := true
scalafmtCheck := true
scalafmtSbtCheck := true

/* Dependencies */

lazy val googleMaps         = "com.google.maps"   % "google-maps-services" % "0.2.7"
lazy val squants            = "org.typelevel"     %% "squants"             % "1.3.0"
lazy val cats               = "org.typelevel"     %% "cats-core"           % "1.1.0"
lazy val `cats-effect`      = "org.typelevel"     %% "cats-effect"         % "1.0.0-RC"
lazy val `cats-par`         = "io.chrisdavenport" %% "cats-par"            % "0.1.0"
lazy val enumeratum         = "com.beachape"      %% "enumeratum"          % "1.5.13"
lazy val `enumeratum-circe` = "com.beachape"      %% "enumeratum-circe"    % "1.5.17"

lazy val scalacache = ((version: String) =>
  Seq(
    "com.github.cb372" %% "scalacache-core"        % version,
    "com.github.cb372" %% "scalacache-cats-effect" % version,
    "com.github.cb372" %% "scalacache-circe"       % version,
    "com.github.cb372" %% "scalacache-caffeine"    % version,
    "com.github.cb372" %% "scalacache-redis"       % version
  ))("0.24.1")

lazy val circe = ((version: String) =>
  Seq(
    "io.circe" %% "circe-generic" % version,
    "io.circe" %% "circe-java8"   % version,
  ))("0.9.3")

lazy val testKit = {
  val kantancsv = ((version: String) =>
    Seq(
      "com.nrinaudo" %% "kantan.csv"         % version,
      "com.nrinaudo" %% "kantan.csv-cats"    % version,
      "com.nrinaudo" %% "kantan.csv-generic" % version
    ))("0.4.0")

  Seq(
    "org.scalacheck" %% "scalacheck"            % "1.14.0",
    "org.scalatest"  %% "scalatest"             % "3.0.5",
    "com.beachape"   %% "enumeratum-scalacheck" % "1.5.15",
    "io.circe"       %% "circe-literal"         % "0.9.3",
    "io.monix"       %% "monix"                 % "3.0.0-RC1"
  ) ++ kantancsv
}.map(_ % Test)

libraryDependencies ++= Seq(
  squants,
  cats,
  `cats-effect`,
  `cats-par`,
  enumeratum,
  `enumeratum-circe`,
  googleMaps
) ++ circe ++ scalacache ++ testKit

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
