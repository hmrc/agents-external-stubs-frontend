import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs-frontend",
    organization := "uk.gov.hmrc",
    scalacOptions ++= Seq(
      "-Xlint:-missing-interpolator,_",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=routes/.*:s", // silence warnings from routes files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
      "-language:implicitConversions"
    ),
    PlayKeys.playDefaultPort := 9099,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
    CodeCoverageSettings.scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .settings(
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin)
  .dependsOn(root % "test->test")
  .settings(DefaultBuildSettings.itSettings())
