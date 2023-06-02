object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "uk.gov.hmrc.BuildInfo",
    "Reverse.*",
    "app.assets.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )

  private val excludedFiles: Seq[String] = Seq(
    "*.template",
    "SpecialCasesController",
    "MicroserviceAuditConnector",
    "GraphiteStartUp"
  )

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
      ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80.00,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }
}
