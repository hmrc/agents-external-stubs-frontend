import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer = "7.15.0"

  lazy val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVer,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.7.0-play-28",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "1.2.0",
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "agent-kenshoo-monitoring"   % "5.3.0",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2"
  )

  lazy val test =
    Seq(
      "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVer % "test, it",
      "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"      % "test, it",
      "org.scalatestplus"      %% "mockito-3-12"           % "3.2.10.0"   % "test, it",
      "com.github.tomakehurst"  % "wiremock-jre8"          % "2.26.1"     % "test, it",
      "com.vladsch.flexmark"    % "flexmark-all"           % "0.35.10"    % "test, it"
    )

}