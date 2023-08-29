import CompileFlags._
import DependenciesScopesHandler._
import Dependencies._
import PublishSettings.localCacheSettings
import org.typelevel.scalacoptions.ScalacOptions

lazy val scala213 = "2.13.11"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild {
  List(
    organization      := "com.colisweb",
    scalaVersion      := scala213,
    scalafmtOnCompile := true,
    scalafmtCheck     := true,
    scalafmtSbtCheck  := true,
    Test / fork       := true,
    scalacOptions ++= crossScalacOptions(scalaVersion.value),
    localCacheSettings
  )
}
//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `here-provider`, `memory-guava`, `redis-cache`, tests)

lazy val core = project
  .settings(moduleName := "scala-distances-core")
  .settings(Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement)
  .settings(libraryDependencies ++= compileDependencies(squants, simplecacheWrapperCats))
  .settings(
    libraryDependencies ++= testDependencies(
      mockitoScalaScalatest,
      scalacheck,
      scalatest,
      scalatestPlus,
      simplecacheMemory
    )
  )

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-provider-google")
  .settings(
    libraryDependencies ++= compileDependencies(enumeratum, googleMaps, loggingInterceptor, refined)
  )
  .dependsOn(core)

lazy val `here-provider` = project
  .in(file("providers/here"))
  .settings(moduleName := "scala-distances-provider-here")
  .settings(
    libraryDependencies ++= compileDependencies(
      circe,
      circeGeneric,
      circeGenericExtras,
      circeJawn,
      logstashLogbackEncode,
      refined,
      requests
    )
  )
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-cache-redis")
  .settings(libraryDependencies ++= compileDependencies(simplecacheRedisCirce))
  .dependsOn(core)

lazy val `memory-guava` = project
  .in(file("caches/memory-guava"))
  .settings(moduleName := "scala-distances-memory-guava")
  .settings(libraryDependencies ++= compileDependencies(simplecacheMemoryGuava))
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .settings(Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement)
  .dependsOn(core % "test->test;compile->compile", `google-provider`, `here-provider`, `redis-cache`, `memory-guava`)
  .settings(libraryDependencies ++= testDependencies(pureconfig, refinedPureconfig, simplecacheRedisCirce, approvals))

/** Copied from Cats */
lazy val noPublishSettings = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)
