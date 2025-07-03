import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val bootstrapVer = "9.13.0"

  lazy val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVer,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.6.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "2.2.0",
    "uk.gov.hmrc"       %% "play-partials-play-30"      % "10.1.0",
    "org.playframework" %% "play-json-joda"             % "3.0.4"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVer % Test
  )

}
