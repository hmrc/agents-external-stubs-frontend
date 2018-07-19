package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.{AgentsExternalStubsConnector, AuthenticatedSession}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SignInController @Inject()(
  override val messagesApi: MessagesApi,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with I18nSupport {

  import SignInController._

  def showSignInPage(continue: Option[ContinueUrl], origin: String, accountType: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(html.sign_in(SignInRequestForm, routes.SignInController.signIn(continue, origin, accountType)))
    }

  def signIn(continue: Option[ContinueUrl], origin: String, accountType: Option[String] = None): Action[AnyContent] =
    Action.async { implicit request =>
      SignInRequestForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(html.sign_in(formWithErrors, routes.SignInController.signIn(continue, origin, accountType)))),
          credentials =>
            for {
              authenticatedSession <- agentsExternalStubsConnector.signIn(credentials)
              result <- Future.successful(
                         continue.fold(
                           withNewSession(Ok("Authenticated."), authenticatedSession)
                         )(continueUrl =>
                           withNewSession(Redirect(continueUrl.url, Map.empty, 303), authenticatedSession)))
            } yield result
        )
    }

  private def withNewSession(result: Result, session: AuthenticatedSession)(
    implicit request: Request[AnyContent]): Result =
    result.withSession(
      request.session +
        (SessionKeys.sessionId -> UUID.randomUUID().toString) +
        (SessionKeys.authToken -> session.authToken) +
        (SessionKeys.userId    -> session.userId))

}

object SignInController {

  case class SignInRequest(userId: String, plainTextPassword: String, providerType: String = "GovernmentGateway")
  object SignInRequest {
    implicit val writes: Writes[SignInRequest] = Json.writes[SignInRequest]
  }

  val SignInRequestForm: Form[SignInRequest] = Form[SignInRequest](
    mapping(
      "userId"       -> nonEmptyText,
      "password"     -> nonEmptyText,
      "providerType" -> optional(nonEmptyText).transform[String](_.getOrElse("GovernmentGateway"), Some(_))
    )(SignInRequest.apply)(SignInRequest.unapply))
}
