import sbt._

object TestVersions {
  final val mockitoScala  = "1.17.12"
  final val kantan        = "0.7.0"
  final val scalacheck    = "1.17.0"
  final val scalatest     = "3.2.14"
  final val scalatestPlus = "3.1.0.0-RC2"
  final val enumeratum    = "1.7.0"
}

object TestDependencies {
  final val mockitoScalaScalatest = "org.mockito"    %% "mockito-scala-scalatest" % TestVersions.mockitoScala
  final val kantan                = "com.nrinaudo"   %% "kantan.csv"              % TestVersions.kantan     % Test
  final val kantanCats            = "com.nrinaudo"   %% "kantan.csv-cats"         % TestVersions.kantan     % Test
  final val kantanGeneric         = "com.nrinaudo"   %% "kantan.csv-generic"      % TestVersions.kantan     % Test
  final val circeLiteral          = "io.circe"       %% "circe-literal"           % Versions.circe          % Test
  final val scalacheck            = "org.scalacheck" %% "scalacheck"              % TestVersions.scalacheck % Test
  final val scalatestPlus        = "org.scalatestplus" %% "scalatestplus-scalacheck" % TestVersions.scalatestPlus % Test
  final val scalatest            = "org.scalatest"     %% "scalatest"                % TestVersions.scalatest     % Test
  final val enumeratumScalacheck = "com.beachape"      %% "enumeratum-scalacheck"    % TestVersions.enumeratum    % Test
}
