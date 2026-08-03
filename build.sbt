import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs-frontend",
    organization := "uk.gov.hmrc",
    scalacOptions ++= Seq(
      "-feature",
      "-Werror",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=routes/.*:s", // silence warnings from routes files
      "-Wconf:src=.*html.*&msg=Implicit parameters should be provided with a .*using.* clause:s", // silence the Twirl/implicit syntax warning only
    ),
    PlayKeys.playDefaultPort := 9099,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
    CodeCoverageSettings.scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .settings(
    Compile / scalacOptions := (Compile / scalacOptions).value.distinct,
    Test / scalacOptions := (Test / scalacOptions).value.distinct
  )
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin)
  .dependsOn(root % "test->test")
  .settings(
    DefaultBuildSettings.itSettings(),
    Compile / scalacOptions := (Compile / scalacOptions).value.distinct,
    Test / scalacOptions := (Test / scalacOptions).value.distinct
  )
