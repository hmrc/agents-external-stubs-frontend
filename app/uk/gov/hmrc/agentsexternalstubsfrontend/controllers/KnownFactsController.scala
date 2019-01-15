package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.EnrolmentKey
import uk.gov.hmrc.agentsexternalstubsfrontend.services.{Features, ServicesDefinitionsService}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KnownFactsController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val servicesDefinitionsService: ServicesDefinitionsService,
  val features: Features,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  def showKnownFactsPage(enrolmentKey: String): Action[AnyContent] = Action.async { implicit request =>
    EnrolmentKey(enrolmentKey).fold(
      _ =>
        Future.successful(
          Ok(
            html
              .error_template(
                messagesApi("knownFacts.title"),
                "Invalid request",
                "Provided enrolment key format is not valid."))),
      _ =>
        authorised()
          .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
            agentsExternalStubsConnector
              .getKnownFacts(enrolmentKey)
              .map(
                enrolmentInfo =>
                  Ok(
                    html.show_known_facts(
                      enrolmentInfo,
                      servicesDefinitionsService.servicesDefinitions
                        .getService(enrolmentInfo.enrolmentKey.service)
                        .getOrElse(throw new Exception()),
                      pageContext(credentials)
                    )
                )
              )
              .recover {
                case _: NotFoundException =>
                  Ok(
                    html
                      .error_template(
                        messagesApi("knownFacts.title"),
                        "Known facts not found",
                        "Known facts for the provided enrolment key does not exist on this test planet."))
              }
        }
    )
  }

  val showEnrolmentsPage: Action[AnyContent] = Action.async { implicit request =>
    if (features.showEnrolments)
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          Future.successful(
            Ok(
              html.show_all_services(servicesDefinitionsService.servicesDefinitions, pageContext(credentials))
            ))
        } else Future.successful(Forbidden)
  }

}
