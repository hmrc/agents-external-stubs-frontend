import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-27" % "2.24.0",
  "uk.gov.hmrc"       %% "govuk-template"             % "5.56.0-play-27",
  "uk.gov.hmrc"       %% "play-ui"                    % "8.11.0-play-27",
  "uk.gov.hmrc"       %% "auth-client"                % "3.0.0-play-27",
  "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "0.23.0-play-27",
  "uk.gov.hmrc"       %% "play-partials"              % "6.11.0-play-27",
  "uk.gov.hmrc"       %% "agent-kenshoo-monitoring"   % "4.4.0",
  "com.typesafe.play" %% "play-json-joda"             % "2.7.4"
)

def testDeps(scope: String) =
  Seq(
    "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26" % scope,
    "org.scalatest"          %% "scalatest"          % "3.0.8"         % scope,
    "org.mockito"             % "mockito-core"       % "3.2.0"         % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"         % scope,
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.1"        % scope
  )

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    majorVersion := 0,
    PlayKeys.playDefaultPort := 9099,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    routesImport += "uk.gov.hmrc.play.binders._",
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true
  )
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true
  )
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
