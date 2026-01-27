import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val playVer = "play-30"
  private val bootstrapVer = "10.5.0"

  lazy val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVer" % bootstrapVer,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVer" % "12.27.0",
    "uk.gov.hmrc"       %% s"play-partials-$playVer"      % "10.2.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"        % "2.2.0",
    "org.playframework" %% "play-json-joda"               % "3.0.4"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVer" % bootstrapVer
  ).map(_ % Test)

}
