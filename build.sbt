
lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    scalacOptions ++= Seq(
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=routes/.*:s", // silence warnings from routes files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
      "-language:implicitConversions"
    ),
    majorVersion := 0,
    PlayKeys.playDefaultPort := 9099,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    routesImport += "uk.gov.hmrc.play.bootstrap.binders._",
    CodeCoverageSettings.scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(
    Test / parallelExecution := false,
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)

inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
