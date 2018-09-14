package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.services.ServicesDefinitionsService
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class KnownFactsController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector,
  servicesDefinitionsService: ServicesDefinitionsService
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  def showKnownFactsPage(enrolmentKey: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      agentsExternalStubsConnector
        .getKnownFacts(enrolmentKey)
        .map(
          enrolmentInfo =>
            Ok(
              html.show_known_facts(
                enrolmentInfo,
                servicesDefinitionsService.servicesDefinitions
                  .getService(enrolmentInfo.enrolmentKey.service)
                  .getOrElse(throw new Exception()))
          )
        )
    }
  }

  val showGuideToServicesPage: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(
        Ok(
          html.show_all_services(servicesDefinitionsService.servicesDefinitions)
        ))
    }
  }

}
