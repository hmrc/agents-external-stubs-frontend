package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class LoginController @Inject()(
  override val messagesApi: MessagesApi,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with I18nSupport {

  import LoginController._

  def showLogin(continueUrlOpt: Option[ContinueUrl], origin: String, accountType: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(html.login_form(LoginForm, routes.LoginController.login(continueUrlOpt, origin, accountType)))
    }

  def login(
    continueUrlOpt: Option[ContinueUrl],
    origin: String,
    accountType: Option[String] = None): Action[AnyContent] =
    Action.async { implicit request =>
      LoginForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(html.login_form(formWithErrors, routes.LoginController.login(continueUrlOpt, origin, accountType)))),
          credentials =>
            for {
              session <- agentsExternalStubsConnector.login(credentials)
              result <- Future.successful(
                         continueUrlOpt.fold(
                           Ok("Authenticated.").withSession(
                             request.session + (SessionKeys.sessionId -> session.sessionId)
                           )
                         )(continueUrl =>
                           Redirect(continueUrl.url, Map.empty, 303).withSession(
                             request.session + (SessionKeys.sessionId -> session.sessionId)
                         )))
            } yield result
        )
    }

}

object LoginController {

  case class Credentials(userId: String, plainTextPassword: String)
  object Credentials {
    implicit val writes: Writes[Credentials] = Json.writes[Credentials]
  }

  val LoginForm: Form[Credentials] = Form[Credentials](
    mapping(
      "userId"   -> nonEmptyText,
      "password" -> nonEmptyText
    )(Credentials.apply)(Credentials.unapply))
}
