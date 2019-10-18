package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.Forms.text
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.PersonalDetailsValidationController.PdvRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

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
        val nino: String = currentUser.nino.map(_.value).getOrElse("")
        Ok(
          html.pdv_start(
            PersonalDetailsValidationController.PdvRequestForm.fill(PdvRequest(nino)),
            routes.PersonalDetailsValidationController.submit(completionUrl)
          ))
      }
    }
  }

  def submit(completionUrl: ContinueUrl): Action[AnyContent] = Action {
    Redirect(completionUrl.url, Map("validationId" -> Seq("1234567890")))
  }
}

object PersonalDetailsValidationController {

  case class PdvRequest(nino: String)

  object PdvRequest {
    implicit val format: OFormat[PdvRequest] = Json.format
  }

  val PdvRequestForm: Form[PdvRequest] = Form[PdvRequest](
    mapping(
      "nino" -> text.verifying("Enter a valid Nino", v => Nino.isValid(v.replace(" ", "")))
    )(PdvRequest.apply)(PdvRequest.unapply)
  )
}
