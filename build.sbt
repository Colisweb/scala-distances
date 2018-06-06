import sbt.Keys.crossScalaVersions

organization in ThisBuild := "com.guizmaii"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalaVersion in ThisBuild := scala212
crossScalaVersions in ThisBuild := Seq(scala211, scala212)

scalafmtOnCompile in ThisBuild := true
scalafmtCheck in ThisBuild := true
scalafmtSbtCheck in ThisBuild := true

//// Dependencies

lazy val googleMaps      = "com.google.maps"   % "google-maps-services" % "0.2.7"
lazy val squants         = "org.typelevel"     %% "squants"             % "1.3.0"
lazy val cats            = "org.typelevel"     %% "cats-core"           % "1.1.0"
lazy val `cats-effect`   = "org.typelevel"     %% "cats-effect"         % "1.0.0-RC"
lazy val `cats-par`      = "io.chrisdavenport" %% "cats-par"            % "0.1.0"
lazy val enumeratum      = "com.beachape"      %% "enumeratum"          % "1.5.13"
lazy val `circe-generic` = "io.circe"          %% "circe-generic"       % "0.9.3"
lazy val monix           = "io.monix"          %% "monix"               % "3.0.0-RC1"

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
    monix
  ) ++ kantancsv
}.map(_ % Test)

//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`, tests, benchmark)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`, tests, benchmark)

lazy val core = project
  .settings(moduleName := "scala-distances")
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(
    libraryDependencies ++= Seq(
      squants,
      cats,
      `cats-effect`,
      `cats-par`,
      enumeratum,
      `circe-generic`
    ) ++ scalacache ++ testKit)

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-google")
  .settings(libraryDependencies += googleMaps)
  .dependsOn(core)

lazy val `maths-provider` = project
  .in(file("providers/maths"))
  .settings(moduleName := "scala-distances-maths")
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-redis")
  .dependsOn(core)

lazy val `caffeine-cache` = project
  .in(file("caches/caffeine"))
  .settings(moduleName := "scala-distances-caffeine")
  .dependsOn(core)

lazy val `no-cache` = project
  .in(file("caches/no-cache"))
  .settings(moduleName := "scala-distances-noCache")
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= testKit)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, `no-cache`)

lazy val benchmark = project
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
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    scmInfo := Some(ScmInfo(url("https://github.com/guizmaii/scala-distances"), "scm:git:git@github.com:guizmaii/scala-distances.git")),
    homepage := Some(url("https://github.com/guizmaii/scala-distances")),
    developers := List(Developer("guizmaii", "Jules Ivanic", "jules.ivanic@gmail.com", url("https://guizmaii.github.io/"))),
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher
  )
)

//// Aliases

/**
  * Copied from kantan.csv
  */
addCommandAlias("runBench", "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1")
