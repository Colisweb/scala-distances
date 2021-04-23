import sbt.Keys._
import sbt.Scoped.ScopingSetting
import sbt._
import xerial.sbt.Sonatype.autoImport._

object ReleaseSettings {
  def globalReleaseSettings: SettingsDefinition =
    Seq(
      sonatypeCredentialHost := "s01.oss.sonatype.org",
      sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
      publishConfiguration := publishConfiguration.value.withOverwrite(true),
      publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
    )

  def buildReleaseSettings(
      details: String,
      licenseName: String,
      licenseUrl: String,
      projectName: String,
      projectScm: String = "git@gitlab.com:colisweb-open-source",
      organizationHome: String = "https://gitlab.com/colisweb-open-source",
      directory: Option[String] = Some("scala")
  ): SettingsDefinition = {
    val dir = directory.map("/" + _ + "/").getOrElse("/")
    inThisBuild(Seq(
      organization := "com.colisweb",
      organizationName := "Colisweb",
      organizationHomepage := Some(url(organizationHome)),
      credentials += Credentials(
        "Sonatype Nexus Repository Manager",
        "s01.oss.sonatype.org",
        sys.env.getOrElse("SONATYPE_USERNAME", "unused"),
        sys.env.getOrElse("SONATYPE_PASSWORD", "unused")
      ),
      version := sys.env.getOrElse("CI_COMMIT_TAG", "0.0.1-SNAPSHOT").replaceAll("v", ""),
      //Per build generic configuration
      publishMavenStyle := true,
      // Remove all additional repository other than Maven Central from POM
      pomIncludeRepository := { _ => false },
      publishTo := {
        if ((ThisProject / isSnapshot).value)
          Some("snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots")
        else (ThisProject / sonatypePublishToBundle).value
      },
      scmInfo := Some(
        ScmInfo(
          url(s"$organizationHome$dir$projectName"),
          s"scm:$projectScm$dir$projectName.git"
        )
      ),
      description := details,
      licenses := Seq(licenseName -> url(licenseUrl)),
      homepage := Some(url(s"$organizationHome$dir$projectName"))
    ))
  }

}

object Developers {
  val michelDaviot = Developer(
    id = "tyrcho",
    name = "Michel Daviot",
    email = "michel.daviot@colisweb.com",
    url = url("https://github.com/tyrcho")
  )

  val cyrilVerdier = Developer(
    id = "cverdier.colisweb",
    name = "Cyril Verdier",
    email = "cyril.verdier@colisweb.com",
    url = url("https://gitlab.com/cverdier.colisweb")
  )

  val julesIvanic = Developer(
    id = "guizmaii",
    name = "Jules Ivanic",
    email = "none",
    url = url("https://guizmaii.github.io/")
  )

  val colasMombrun = Developer(
    id = "LitlBro",
    name = "Colas Mombrun",
    email = "colas.mombrun@colisweb.com",
    url = url("https://gitlab.com/LitlBro")
  )

  val simonBar = Developer(
    id = "snatz",
    name = "Simon Bar",
    email = "simon.bar@colisweb.com",
    url = url("https://gitlab.com/snatz")
  )

  val sallaReznov = Developer(
    id = "sallareznov",
    name = "Salla Reznov",
    email = "salla@colisweb.com",
    url = url("https://gitlab.com/sallareznov")
  )
}
