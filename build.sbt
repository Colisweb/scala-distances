import CompileFlags._
import sbt.Keys.crossScalaVersions
import DependenciesScopesHandler._
import Dependencies._

lazy val scala212               = "2.12.13"
lazy val scala213               = "2.13.10"
lazy val supportedScalaVersions = List(scala213, scala212)

Global / onChangedBuildSource  := ReloadOnSourceChanges
ThisBuild / organization       := "com.colisweb"
ThisBuild / scalaVersion       := scala213
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / scalafmtOnCompile  := true
ThisBuild / scalafmtCheck      := true
ThisBuild / scalafmtSbtCheck   := true
ThisBuild / scalacOptions ++= crossScalacOptions(scalaVersion.value)

ThisBuild / pushRemoteCacheTo := Some(
  MavenCache("local-cache", baseDirectory.value / sys.env.getOrElse("CACHE_PATH", "sbt-cache"))
)
//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `here-provider`, `redis-cache`, `caffeine-cache`, tests)
  .dependsOn(core, `google-provider`, `here-provider`, `redis-cache`, `caffeine-cache`, tests)

lazy val core = project
  .settings(moduleName := "scala-distances-core")
  .settings(
    libraryDependencies ++= compileDependencies(
      cats,
      scalaCache,
      squants
    ) ++ testDependencies(
      monix,
      kantan,
      kantanCats,
      kantanGeneric,
      mockitoScalaScalatest,
      circeLiteral,
      scalacheck,
      scalatestPlus,
      scalatest,
      enumeratumScalacheck
    )
  )

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-provider-google")
  .settings(
    libraryDependencies ++= compileDependencies(
      scalaCompat,
      catsEffect,
      googleMaps,
      enumeratum,
      loggingInterceptor,
      refined
    )
  )
  .dependsOn(core)

lazy val `here-provider` = project
  .in(file("providers/here"))
  .settings(moduleName := "scala-distances-provider-here")
  .settings(
    libraryDependencies ++= compileDependencies(
      scalaCompat,
      catsEffect,
      circeJawn,
      requests,
      refined,
      logstashLogbackEncode,
      circe,
      circeGeneric,
      circeGenericExtras,
    )
  )
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-cache-redis")
  .settings(libraryDependencies ++= compileDependencies(scalaCacheRedis))
  .dependsOn(core)

lazy val `caffeine-cache` = project
  .in(file("caches/caffeine"))
  .settings(moduleName := "scala-distances-cache-caffeine")
  .settings(libraryDependencies ++= compileDependencies(scalaCacheCaffeine))
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .dependsOn(core % "test->test;compile->compile", `google-provider`, `here-provider`, `redis-cache`, `caffeine-cache`)
  .settings(libraryDependencies ++= testDependencies(pureconfig, refinedPureconfig, scalaCacheCatsEffect))

/** Copied from Cats
  */
lazy val noPublishSettings = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)
