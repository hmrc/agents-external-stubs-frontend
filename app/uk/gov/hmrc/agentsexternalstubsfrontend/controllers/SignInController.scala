/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.sign_in
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

@Singleton
class SignInController @Inject() (
  override val messagesApi: MessagesApi,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  signInView: sign_in,
  val authConnector: AuthConnector,
  ecp: Provider[ExecutionContext],
  sessionCookieBaker: SessionCookieBaker
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport {

  implicit val ec: ExecutionContext = ecp.get

  import SignInController._

  def showSignInPage(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm,
          routes.SignInController
            .signIn(continue, origin, accountType, providerType = AuthProvider.GovernmentGateway)
        )
      )
    }

  def register(
    continueUrl: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    showSignInPage(continueUrl, origin, accountType)

  def showSignInPageSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String]
  ): Action[AnyContent] =
    showSignInPage(continue_url, origin, None)

  def showGovernmentGatewaySignInPage(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ) = showSignInPage(continue, origin, accountType)

  def signInSsoSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    signInSsoImpl(continue_url, origin, accountType, false)

  def signInSsoInternalSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    signInSsoImpl(continue_url, origin, accountType, true)

  private def signInSsoImpl(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String],
    internal: Boolean
  ): Action[AnyContent] =
    Action.async { implicit request =>
      (for {
        authenticatedSession <- agentsExternalStubsConnector.currentSession()
        result <- Future(
                    continue_url.fold(
                      Redirect(routes.UserController.showUserPage(None))
                    )(RedirectUrl => Redirect(RedirectUrl.unsafeValue))
                  )
      } yield result)
        .recover { case _ =>
          Redirect(
            if (internal)
              routes.SignInController.showSignInPageInternalSCP(continue_url, origin)
            else
              routes.SignInController.showSignInPageSCP(continue_url, origin)
          )
        }
    }

  def signIn(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String],
    providerType: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      SignInRequestForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(
                signInView(formWithErrors, routes.SignInController.signIn(continue, origin, accountType, providerType))
              )
            ),
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
                              )(RedirectUrl => Redirect(RedirectUrl.unsafeValue)),
                            authenticatedSession
                          )
                        )
            } yield result
        )
    }

  def showSignInStridePage(
    successURL: RedirectUrl,
    origin: Option[String],
    failureURL: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm,
          routes.SignInController
            .signIn(Some(successURL), origin, None, providerType = AuthProvider.PrivilegedApplication)
        )
      )
    }

  def signInUser(continue: Option[RedirectUrl], userId: String, providerType: String): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          for {
            _ <- agentsExternalStubsConnector.signOut()
            authenticatedSession <-
              agentsExternalStubsConnector.signIn(
                SignInRequest(userId = userId, planetId = credentials.planetId, providerType = providerType)
              )
            result <- Future(
                        withNewSession(
                          if (authenticatedSession.newUserCreated.getOrElse(false))
                            Redirect(routes.UserController.showCreateUserPage(continue))
                          else
                            continue.fold(
                              Redirect(routes.UserController.showUserPage(None))
                            )(RedirectUrl => Redirect(RedirectUrl.unsafeValue)),
                          authenticatedSession
                        )
                      )
          } yield result
        }
    }

  def signOut(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPage(None, None, None).url)
              .discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME))
          )(c => Redirect(c.unsafeValue).discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME)))
        )
    }

  def signOutSCP(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPageSCP(None, None).url)
              .discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME))
          )(c => Redirect(c.unsafeValue).discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME)))
        )
    }

  private def withNewSession(result: Result, session: AuthenticatedSession)(implicit
    request: Request[AnyContent]
  ): Result =
    result.withSession(
      request.session +
        (SessionKeys.sessionId            -> session.sessionId) +
        (SessionKeys.authToken            -> s"Bearer ${session.authToken}") +
        (SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString) +
        ("planetId"                       -> session.planetId)
    )

  def showSignInPageInternal(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm,
          routes.SignInController
            .signInInternal(continue, origin, accountType, providerType = AuthProvider.GovernmentGateway)
        )
      )
    }

  def showSignInPageInternalSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String]
  ): Action[AnyContent] =
    showSignInPageInternal(continue_url, origin, None)

  def registerInternal(
    RedirectUrl: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    showSignInPageInternal(RedirectUrl, origin, accountType)

  def showGovernmentGatewaySignInPageInternal(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ) = showSignInPageInternal(continue, origin, accountType)

  def showSignInStridePageInternal(
    successURL: RedirectUrl,
    origin: Option[String],
    failureURL: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm,
          routes.SignInController
            .signInInternal(Some(successURL), origin, None, providerType = AuthProvider.PrivilegedApplication)
        )
      )
    }

  def signInInternal(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String] = None,
    providerType: String
  ): Action[AnyContent] =
    signIn(continue, origin, accountType, providerType)

  def signOutInternal(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPageInternal(None, None, None).url)
              .discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME))
          )(c => Redirect(c.unsafeValue).discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME)))
        )
    }

  def signOutInternalSCP(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPageInternalSCP(None, None).url)
              .discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME))
          )(c => Redirect(c.unsafeValue).discardingCookies(DiscardingCookie(sessionCookieBaker.COOKIE_NAME)))
        )
    }

}

object SignInController {

  case class SignInRequest(
    userId: String,
    planetId: String,
    plainTextPassword: String = "p@ssw0rd",
    providerType: String = AuthProvider.GovernmentGateway,
    syncToAuthLoginApi: Boolean = true
  )

  object SignInRequest {
    implicit val writes: Writes[SignInRequest] = Json.writes[SignInRequest]
  }

  val SignInRequestForm: Form[SignInRequest] = Form[SignInRequest](
    mapping(
      "userId"             -> nonEmptyText,
      "planetId"           -> nonEmptyText,
      "password"           -> default(text, "p@ssw0rd"),
      "providerType"       -> optional(nonEmptyText).transform[String](_.getOrElse(AuthProvider.GovernmentGateway), Some(_)),
      "syncToAuthLoginApi" -> ignored(true)
    )(SignInRequest.apply)(SignInRequest.unapply)
  )
}
