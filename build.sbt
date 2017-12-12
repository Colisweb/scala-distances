organization := "com.guizmaii"

name := "scala-distances"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.12", scalaVersion.value)

scalafmtOnCompile := true

scalacOptions ++= Seq(
  "-deprecation",
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  //  "-Xfatal-warnings",
  //  "-Ywarn-unused-import"
  "-Xlint",
  "-Xlint:missing-interpolator",
  "-Yno-adapted-args",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

val monix      = "io.monix"        %% "monix"               % "3.0.0-M2"
val googleMaps = "com.google.maps" % "google-maps-services" % "0.2.5"
val squants    = "org.typelevel"   %% "squants"             % "1.3.0"

val scalacache = ((version: String) =>
  Seq(
    "com.github.cb372" %% "scalacache-core"     % version,
    "com.github.cb372" %% "scalacache-caffeine" % version,
    "com.github.cb372" %% "scalacache-redis"    % version,
    "com.github.cb372" %% "scalacache-monix"    % version
  ))("0.21.0")

val testKit = Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  "org.scalatest"  %% "scalatest"  % "3.0.4"
)

libraryDependencies ++= Seq(
  monix   % Provided,
  squants % Provided,
  googleMaps
) ++ scalacache ++ testKit.map(_ % Test)

// sbt-release-early
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
