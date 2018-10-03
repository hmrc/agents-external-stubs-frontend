package uk.gov.hmrc.agentsexternalstubsfrontend.services
import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Features @Inject()(configuration: Configuration) {

  lazy val showRestQuery: Boolean = configuration
    .getBoolean("features.show-rest-query")
    .getOrElse(true)

  lazy val showEnrolments: Boolean = configuration
    .getBoolean("features.show-enrolments")
    .getOrElse(true)
}
