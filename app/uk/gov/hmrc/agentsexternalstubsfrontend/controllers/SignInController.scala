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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.{AgentsExternalStubsConnector, AuthenticatedSession}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.{quick_start_agents_hub, sign_in}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.agentsexternalstubsfrontend.forms._
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features

@Singleton
class SignInController @Inject() (
  override val messagesApi: MessagesApi,
  appConfig: FrontendConfig,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  signInView: sign_in,
  quickStartView: quick_start_agents_hub,
  val authConnector: AuthConnector,
  val features: Features,
  ecp: Provider[ExecutionContext],
  sessionCookieBaker: SessionCookieBaker
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  def showQuickStart(): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          Future.successful(
            Ok(
              quickStartView(
                appConfig.quickStartHubBaseUrl,
                appConfig.quickStartHubStrideBaseUrl,
                pageContext(credentials)
              )
            )
          )
        }
    }

  def showSignInPage(
    continue: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm.form,
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
  ): Action[AnyContent] = showSignInPage(continue, origin, accountType)

  def signInSsoSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    signInSsoImpl(continue_url, origin, internal = false)

  def signInSsoInternalSCP(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    accountType: Option[String]
  ): Action[AnyContent] =
    signInSsoImpl(continue_url, origin, internal = true)

  private def signInSsoImpl(
    continue_url: Option[RedirectUrl],
    origin: Option[String],
    internal: Boolean
  ): Action[AnyContent] =
    Action.async { implicit request =>
      (for {
        _ <- agentsExternalStubsConnector.currentSession()
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
      SignInRequestForm.form
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
          SignInRequestForm.form,
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
            Redirect(routes.SignInController.showSignInPage(None, None, None).url).withNewSession
          )(c => Redirect(c.unsafeValue).withNewSession)
        )
    }

  def signOutSCP(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPageSCP(None, None).url).withNewSession
          )(c => Redirect(c.unsafeValue).withNewSession)
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
          SignInRequestForm.form,
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
  ): Action[AnyContent] = showSignInPageInternal(continue, origin, accountType)

  def showSignInStridePageInternal(
    successURL: RedirectUrl,
    origin: Option[String],
    failureURL: Option[String]
  ): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        signInView(
          SignInRequestForm.form,
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
            Redirect(routes.SignInController.showSignInPageInternal(None, None, None).url).withNewSession
          )(c => Redirect(c.unsafeValue).withNewSession)
        )
    }

  def signOutInternalSCP(continue: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      agentsExternalStubsConnector
        .signOut()
        .map(_ =>
          continue.fold(
            Redirect(routes.SignInController.showSignInPageInternalSCP(None, None).url).withNewSession
          )(c => Redirect(c.unsafeValue).withNewSession)
        )
    }

}
