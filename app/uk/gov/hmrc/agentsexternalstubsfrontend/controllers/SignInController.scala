package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.{AgentsExternalStubsConnector, AuthenticatedSession}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignInController @Inject()(
  override val messagesApi: MessagesApi,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val authConnector: AuthConnector,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  implicit val ec: ExecutionContext = ecp.get

  import SignInController._

  def showSignInPage(
    continue: Option[ContinueUrl],
    origin: Option[String],
    accountType: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        html
          .sign_in(
            SignInRequestForm,
            routes.SignInController
              .signIn(continue, origin, accountType, providerType = AuthProvider.GovernmentGateway)))
    }

  def showGovernmentGatewaySignInPage(accountType: Option[String], origin: Option[String], continue: Option[ContinueUrl]) =
    Action { implicit request =>
      Ok(
        html
          .sign_in(
            SignInRequestForm,
            routes.SignInController
              .signIn(continue, origin, accountType, providerType = AuthProvider.GovernmentGateway)))
    }

  def signIn(
    continue: Option[ContinueUrl],
    origin: Option[String],
    accountType: Option[String],
    providerType: String): Action[AnyContent] =
    Action.async { implicit request =>
      SignInRequestForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(html
                .sign_in(formWithErrors, routes.SignInController.signIn(continue, origin, accountType, providerType)))),
          credentials =>
            for {
              authenticatedSession <- agentsExternalStubsConnector.signIn(credentials.copy(providerType = providerType))
              result <- Future(
                         withNewSession(
                           if (authenticatedSession.newUserCreated.getOrElse(false))
                             Redirect(routes.UserController.showCreateUserPage(continue))
                           else
                             continue.fold(
                               Redirect(routes.UserController.showUserPage(None))
                             )(continueUrl => Redirect(continueUrl.url)),
                           authenticatedSession
                         ))
            } yield result
        )
    }

  def showSignInStridePage(
    successURL: ContinueUrl,
    origin: Option[String],
    failureURL: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        html
          .sign_in(
            SignInRequestForm,
            routes.SignInController
              .signIn(Some(successURL), origin, None, providerType = AuthProvider.PrivilegedApplication)))
    }

  def signInUser(continue: Option[ContinueUrl], userId: String, providerType: String): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          for {
            _ <- agentsExternalStubsConnector.signOut()
            authenticatedSession <- agentsExternalStubsConnector.signIn(
                                     SignInRequest(
                                       userId = userId,
                                       planetId = credentials.planetId,
                                       providerType = providerType))
            result <- Future(
                       withNewSession(
                         if (authenticatedSession.newUserCreated.getOrElse(false))
                           Redirect(routes.UserController.showCreateUserPage(continue))
                         else
                           continue.fold(
                             Redirect(routes.UserController.showUserPage(None))
                           )(continueUrl => Redirect(continueUrl.url)),
                         authenticatedSession
                       ))
          } yield result
        }
    }

  def signOut(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(Redirect(routes.SignInController.showSignInPage(None, None, None).url).withNewSession)(c =>
            Redirect(c.url).withNewSession))
    }

  private def withNewSession(result: Result, session: AuthenticatedSession)(
    implicit request: Request[AnyContent]): Result =
    result.withSession(
      request.session +
        (SessionKeys.sessionId -> session.sessionId) +
        (SessionKeys.authToken -> s"Bearer ${session.authToken}") +
        (SessionKeys.userId    -> s"/auth/oid/${session.userId}") +
        (SessionKeys.token     -> "deprecated") +
        ("planetId"            -> session.planetId)
    )

  def showSignInPageInternal(
    continue: Option[ContinueUrl],
    origin: Option[String],
    accountType: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        html
          .sign_in(
            SignInRequestForm,
            routes.SignInController
              .signInInternal(continue, origin, accountType, providerType = AuthProvider.GovernmentGateway)))
    }

  def showSignInStridePageInternal(
    successURL: ContinueUrl,
    origin: Option[String],
    failureURL: Option[String]): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        html
          .sign_in(
            SignInRequestForm,
            routes.SignInController
              .signInInternal(Some(successURL), origin, None, providerType = AuthProvider.PrivilegedApplication)))
    }

  def signInInternal(
    continue: Option[ContinueUrl],
    origin: Option[String],
    accountType: Option[String] = None,
    providerType: String): Action[AnyContent] =
    signIn(continue, origin, accountType, providerType)

  def signOutInternal(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(Redirect(routes.SignInController.showSignInPageInternal(None, None, None).url).withNewSession)(
            c => Redirect(c.url).withNewSession))
    }

}

object SignInController {

  case class SignInRequest(
    userId: String,
    planetId: String,
    plainTextPassword: String = "p@ssw0rd",
    providerType: String = AuthProvider.GovernmentGateway)
  object SignInRequest {
    implicit val writes: Writes[SignInRequest] = Json.writes[SignInRequest]
  }

  val SignInRequestForm: Form[SignInRequest] = Form[SignInRequest](
    mapping(
      "userId"       -> nonEmptyText,
      "planetId"     -> nonEmptyText,
      "password"     -> default(text, "p@ssw0rd"),
      "providerType" -> optional(nonEmptyText).transform[String](_.getOrElse(AuthProvider.GovernmentGateway), Some(_))
    )(SignInRequest.apply)(SignInRequest.unapply))
}
