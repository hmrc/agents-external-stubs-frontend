package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationController @Inject()(
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  ecp: Provider[ExecutionContext])(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  implicit val ec: ExecutionContext = ecp.get

  import IdentityVerificationController._

  def showUpliftPage(
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      val initialValues = UpliftRequest(willSucceed = true, confidenceLevel)
      Ok(
        html
          .iv_uplift(
            UpliftRequestForm.fill(initialValues),
            routes.IdentityVerificationController.uplift(completionURL, failureURL)))
    }

  def uplift(completionURL: ContinueUrl, failureURL: ContinueUrl): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(Ok(html
                  .iv_uplift(formWithErrors, routes.IdentityVerificationController.uplift(completionURL, failureURL)))),
              upliftRequest => {
                if (upliftRequest.willSucceed) {
                  for {
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(upliftRequest.confidenceLevel))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield Redirect(completionURL.url)
                } else {
                  Future.successful(Redirect(failureURL.url))
                }
              }
            )
        }
    }
}

object IdentityVerificationController {

  case class UpliftRequest(willSucceed: Boolean, confidenceLevel: Int)

  object UpliftRequest {
    implicit val writes: Writes[UpliftRequest] = Json.writes[UpliftRequest]
  }

  val UpliftRequestForm: Form[UpliftRequest] = Form[UpliftRequest](
    mapping(
      "willSucceed"     -> boolean,
      "confidenceLevel" -> number
    )(UpliftRequest.apply)(UpliftRequest.unapply))
}
