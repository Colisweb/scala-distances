import sbt.Keys.crossScalaVersions

organization in ThisBuild := "com.guizmaii"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalaVersion in ThisBuild := scala212
crossScalaVersions in ThisBuild := Seq(scala211, scala212)

scalafmtOnCompile in ThisBuild := true
scalafmtCheck in ThisBuild := true
scalafmtSbtCheck in ThisBuild := true

/* Dependencies */

lazy val googleMaps      = "com.google.maps"   % "google-maps-services" % "0.2.7"
lazy val squants         = "org.typelevel"     %% "squants"             % "1.3.0"
lazy val cats            = "org.typelevel"     %% "cats-core"           % "1.1.0"
lazy val `cats-effect`   = "org.typelevel"     %% "cats-effect"         % "1.0.0-RC"
lazy val `cats-par`      = "io.chrisdavenport" %% "cats-par"            % "0.1.0"
lazy val enumeratum      = "com.beachape"      %% "enumeratum"          % "1.5.13"
lazy val `circe-generic` = "io.circe"          %% "circe-generic"       % "0.9.3"

lazy val scalacache = ((version: String) =>
  Seq(
    "com.github.cb372" %% "scalacache-core"        % version,
    "com.github.cb372" %% "scalacache-cats-effect" % version,
    "com.github.cb372" %% "scalacache-circe"       % version,
    "com.github.cb372" %% "scalacache-caffeine"    % version,
    "com.github.cb372" %% "scalacache-redis"       % version
  ))("0.24.1")

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

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val core = project
  .in(file("core"))
  .settings(moduleName := "scala-distances")
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(
    libraryDependencies ++= Seq(
      squants,
      cats,
      `cats-effect`,
      `cats-par`,
      enumeratum,
      `circe-generic`,
      googleMaps
    ) ++ scalacache ++ testKit)

//// Publishing settings

/**
  * Come from Cats
  */
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
