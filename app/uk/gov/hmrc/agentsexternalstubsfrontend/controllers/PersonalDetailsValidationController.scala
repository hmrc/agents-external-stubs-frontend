package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.util.UUID

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.PersonalDetailsValidationController.PdvRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationController @Inject()(
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  ecp: Provider[ExecutionContext])(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  def start(completionUrl: ContinueUrl): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
      agentsExternalStubsConnector.getUser(credentials.providerId).map { currentUser =>
        Ok(
          html.pdv_start(
            PersonalDetailsValidationController.PdvRequestForm.fill(PdvRequest(success = true)),
            routes.PersonalDetailsValidationController.submit(completionUrl)
          ))
      }
    }
  }

  def submit(completionUrl: ContinueUrl): Action[AnyContent] = Action.async { implicit request =>
    PersonalDetailsValidationController.PdvRequestForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            Ok(
              html.pdv_start(
                formWithErrors,
                routes.PersonalDetailsValidationController.submit(completionUrl)
              ))
        ),
        pdvRequest => {
          val validationId = UUID.randomUUID().toString
          agentsExternalStubsConnector
            .storePdvResult(validationId, pdvRequest.success)
            .map(_ => Redirect(completionUrl.url, Map("validationId" -> Seq(validationId))))
        }
      )
  }

}

object PersonalDetailsValidationController {

  case class PdvRequest(success: Boolean)

  object PdvRequest {
    implicit val format: OFormat[PdvRequest] = Json.format
  }

  val PdvRequestForm: Form[PdvRequest] = Form[PdvRequest](
    mapping(
      "success" -> boolean
    )(PdvRequest.apply)(PdvRequest.unapply)
  )
}
