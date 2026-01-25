/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.{SelectServiceForm, SignInRequest}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AsaTestJourney.asaTestJourneys
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.agentsexternalstubsfrontend.services.AsaJourneySetupService
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.{journey_data, select_journey, select_service}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AsaJourneySetupController @Inject() (
  selectJourneyView: select_journey,
  journeyDataView: journey_data,
  selectServiceView: select_service,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  asaJourneySetupService: AsaJourneySetupService,
  frontendConfig: FrontendConfig
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext)
    extends FrontendController(mcc) {

  def selectJourney(journey: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      journey.fold(Future.successful(Ok(selectJourneyView(asaTestJourneys))))(testJourney =>
        for {
          authSession <- agentsExternalStubsConnector.signIn()
          hc = HeaderCarrier(sessionId = Some(SessionId(authSession.sessionId)))
          _ <- agentsExternalStubsConnector.removeUser(authSession.userId)(hc, ec)
          journey = AsaTestJourney.toJourney(testJourney)
          user <- createUser(journey)(hc)
          signedIn <- agentsExternalStubsConnector.signIn(
                        SignInRequest(
                          userId = user.userId,
                          planetId = authSession.planetId,
                          providerType =
                            if (journey.signedInUserAffinityGroup.isEmpty)
                              AuthProvider.PrivilegedApplication
                            else AuthProvider.GovernmentGateway
                        )
                      )(hc, ec)

        } yield Redirect(routes.AsaJourneySetupController.showSelectService).withSession(
          request.session +
            (SessionKeys.sessionId            -> signedIn.sessionId) +
            (SessionKeys.authToken            -> s"Bearer ${signedIn.authToken}") +
            (SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString) +
            ("planetId"                       -> signedIn.planetId) +
            ("journey"                        -> testJourney) +
            ("userId"                         -> user.userId)
        )
      )
    }
  }

  private def createMainUser(asaTestJourney: AsaTestJourney)(implicit hc: HeaderCarrier): Future[User] =
    ???

  private def createUser(asaTestJourney: AsaTestJourney)(implicit hc: HeaderCarrier): Future[User] =
    asaTestJourney.signedInUserAffinityGroup.fold(
      agentsExternalStubsConnector
        .createUser(affinityGroup = None, Json.parse(s"""{"strideRoles": ["maintain_agent_relationships"]}"""))
    ) { ag =>
      val serviceKeys =
        if (ag == Agent) List("HMRC-AS-AGENT")
        else asaTestJourney.signedInUserServices.flatMap(_.client.principalServiceKey)
      agentsExternalStubsConnector
        .createUser(
          Some(ag),
          Json.parse(
            s"""{ "assignedPrincipalEnrolments":["${serviceKeys
              .mkString("\",\"")}"]}\"\"\" }"""
          )
        )
        .flatMap(userCreated =>
          userCreated.assignedPrincipalEnrolments
            .find(_.service == "IR-SA")
            .map(_.identifiers.head.value)
            .map(utr => agentsExternalStubsConnector.updateUser(userCreated.copy(utr = Some(utr))))
            .getOrElse(Future.successful(()))
            .map(_ => userCreated)
        )
    }

  def showSelectService(): Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      request.session
        .get("journey")
        .fold(
          Future.successful(Redirect(routes.AsaJourneySetupController.selectJourney()))
        )(journey =>
          if (AsaTestJourney.toJourney(journey).requiresServiceSelect)
            Future.successful(
              Ok(
                selectServiceView(
                  SelectServiceForm.selectServiceFormForJourney(journey),
                  SelectServiceForm.servicesForJourney(journey),
                  journey
                )
              )
            )
          else Future.successful(Redirect(routes.AsaJourneySetupController.createTestData()))
        )
    }
  }

  def submitSelectService: Action[AnyContent] = Action.async { implicit request =>
    request.session
      .get("journey")
      .fold(Future.successful(Redirect(routes.AsaJourneySetupController.selectJourney()))) { journey =>
        SelectServiceForm
          .selectServiceFormForJourney(journey)
          .bindFromRequest()
          .fold(
            hasErrors => Future.successful(BadRequest("invalid submission.")),
            asaService =>
              Future.successful(
                Redirect(routes.AsaJourneySetupController.createTestData())
                  .addingToSession("service" -> asaService.id)
              )
            //asaJourneySetupService
            //.setupJourneyForServiceSelected(journey, asaService)
            //.map(updateSessionAndRedirect(_))
          )
      }
  }

  def createTestData: Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      request.session
        .get("journey")
        .fold(Future.successful(Redirect(routes.AsaJourneySetupController.selectJourney()))) { testJourney =>
          val asaTestJourney = AsaTestJourney.toJourney(testJourney)

          asaTestJourney match {
            case journey: JourneyWithServiceSelected =>
              request.session
                .get("service")
                .fold(Future.successful(Redirect(routes.AsaJourneySetupController.showSelectService)))(svc =>
                  asaJourneySetupService
                    .setupDataForJourneyWithServiceSelected(journey, AsaTestJourney.forId(svc))
                    .map(testData =>
                      Redirect(routes.AsaJourneySetupController.showTestData)
                        .addingToSession("journey-data" -> testData)
                    )
                )
            case journey: JourneyWithoutServiceSelected =>
              asaJourneySetupService
                .setupDataForJourneyWithoutServiceSelected(journey)
                .map(_ =>
                  journey match {
                    case MytaInd | MytaOrg =>
                      Redirect(s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/myta")
                    case Track =>
                      Redirect(s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/track")
                    case AccessGroups        => Redirect("")
                    case UkSubscription      => Redirect("")
                    case MmtarProvideDetails => Redirect("")
                  }
                )

          }

        }

    }

  }

  def showTestData: Action[AnyContent] = Action { implicit request =>
    request.session
      .get("journey")
      .fold(Redirect(routes.AsaJourneySetupController.selectJourney())) { journey =>
        request.session.get("service").fold(Redirect(routes.AsaJourneySetupController.showSelectService)) { service =>
          Ok(
            journeyDataView(
              journey,
              service,
              request.session.get("journey-data").getOrElse(""),
              if (journey == "hmrc-led-deauth")
                s"${frontendConfig.agentHelpdeskFrontendfHost}/manage-agent-authorisation"
              else s"${frontendConfig.acrfTestOnlyUrl}/$journey"
            )
          )
        }
      }
  }

  private def updateSessionAndRedirect(journeySetup: JourneySetup)(implicit request: Request[_]): Result =
    journeySetup.authSession match {
      case Some(authSession) =>
        Redirect(journeySetup.redirectUrl).withSession(
          request.session +
            (SessionKeys.sessionId            -> authSession.sessionId) +
            (SessionKeys.authToken            -> s"Bearer ${authSession.authToken}") +
            (SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString) +
            ("planetId"                       -> authSession.planetId) +
            ("journey-data"                   -> journeySetup.journeyData.getOrElse("")) +
            ("service"                        -> journeySetup.service.getOrElse(""))
        )
      case None =>
        Redirect(journeySetup.redirectUrl).withSession(
          request.session +
            ("journey"      -> journeySetup.journey) +
            ("journey-data" -> journeySetup.journeyData.getOrElse("")) +
            ("service"      -> journeySetup.service.getOrElse(""))
        )
    }

  private def onlyIfFeatureEnabled(f: => Future[Result]) =
    if (frontendConfig.showJourneySetup) {
      f
    } else Future.successful(NotImplemented("Feature not enabled."))

}
