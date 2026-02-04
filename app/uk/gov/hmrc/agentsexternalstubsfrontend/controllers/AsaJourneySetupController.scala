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
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SelectServiceForm
import uk.gov.hmrc.agentsexternalstubsfrontend.models.ASAJourneyService.asaJourneys
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.agentsexternalstubsfrontend.services.AsaJourneySetupService
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.{error_template, journey_data, select_journey, select_service}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AsaJourneySetupController @Inject() (
  selectJourneyView: select_journey,
  journeyDataView: journey_data,
  selectServiceView: select_service,
  errorTemplate: error_template,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  asaJourneySetupService: AsaJourneySetupService,
  frontendConfig: FrontendConfig
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext)
    extends FrontendController(mcc) {

  def root: Action[AnyContent] = Action { _ =>
    Redirect(routes.AsaJourneySetupController.selectJourney())
  }

  def selectJourney(journey: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      journey.fold(Future.successful(Ok(selectJourneyView(asaJourneys)))) { journeyId =>
        ASAJourneyService
          .asaJourneyForId(journeyId)
          .fold(
            Future.successful(
              BadRequest(
                errorTemplate(
                  pageTitleMsgKey = "error.summary.heading",
                  "error.summary.heading",
                  "invalid journey"
                )
              )
            )
          )(journey =>
            asaJourneySetupService
              .setupMainUser(journey)
              .map(authSession =>
                Redirect(routes.AsaJourneySetupController.showSelectService).withSession(
                  request.session +
                    (SessionKeys.sessionId            -> authSession.sessionId) +
                    (SessionKeys.authToken            -> s"Bearer ${authSession.authToken}") +
                    (SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString) +
                    ("planetId"                       -> authSession.planetId) +
                    ("journey"                        -> journeyId) +
                    ("userId"                         -> authSession.userId) -
                    "service" - "journey-data"
                )
              )
          )
      }
    }
  }

  def showSelectService(): Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      withJourney {
        case journey @ (journeyWithService: ASATestJourneyWithServiceSelection) =>
          Future.successful(
            Ok(
              selectServiceView(
                SelectServiceForm.selectServiceFormForJourney(journeyWithService),
                SelectServiceForm.servicesForJourney(journeyWithService),
                journey.id
              )
            )
          )
        case journey =>
          createTestDataAndSetUrl(journey).map(nextUrl =>
            Redirect(nextUrl.nextUrl).addingToSession("journey-data" -> nextUrl.testData.getOrElse(""))
          )
      }
    }
  }

  def submitSelectService: Action[AnyContent] = Action.async { implicit request =>
    withJourney {
      case j: ASATestJourneyWithServiceSelection =>
        SelectServiceForm
          .selectServiceFormForJourney(j)
          .bindFromRequest()
          .fold(
            hasErrors => Future.successful(BadRequest("invalid submission.")),
            asaJourneyService =>
              asaJourneySetupService
                .setupDataForJourneyWithServiceSelected(j, asaJourneyService)
                .map(testData =>
                  Redirect(routes.AsaJourneySetupController.showTestData)
                    .addingToSession("journey-data" -> testData, "service" -> asaJourneyService.friendlyName)
                )
          )
      case _ => Future.successful(Redirect(routes.AsaJourneySetupController.selectJourney()))
    }
  }

  def showTestData: Action[AnyContent] = Action.async { implicit request =>
    onlyIfFeatureEnabled {
      withJourney { journey =>
        withJourneyData { journeyData =>
          Future.successful(
            Ok(
              journeyDataView(
                journey,
                request.session.get("service"),
                journeyData,
                journey match {
                  case HmrcLedDeauth  => s"${frontendConfig.agentHelpdeskFrontendfHost}/manage-agent-authorisation"
                  case UkSubscription => s"${frontendConfig.agentSubscriptionFrontendHost}/agent-subscription/start"
                  case _              => s"${frontendConfig.acrfTestOnlyUrl}/${journey.id}"
                }
              )
            )
          )
        }
      }
    }
  }

  case class NextUrl(nextUrl: String, testData: Option[String])

  private def createTestDataAndSetUrl(journey: ASATestJourney)(implicit rh: RequestHeader): Future[NextUrl] =
    journey match {
      case _: ASATestJourneyWithServiceSelection =>
        Future.successful(NextUrl(routes.AsaJourneySetupController.showSelectService.url, None))
      case journeyWithoutService: ASATestJourneyWithoutServiceSelection =>
        asaJourneySetupService
          .setupDataForJourneyWithoutServiceSelected(journeyWithoutService)
          .map { journeyData =>
            val url = {
              journeyWithoutService match {
                case MytaInd | MytaOrg =>
                  s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/myta"
                case Track =>
                  s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/track"
                case AccessGroups => s"${frontendConfig.asafHost}/agent-services-account/manage-account"
                case UkSubscription =>
                  routes.AsaJourneySetupController.showTestData.url
                case MmtarProvideDetails =>
                  s"${frontendConfig.agentRegistrationFrontendExternalUrl}/agent-registration/provide-details/start/${journeyData
                    .getOrElse("")}"
              }
            }
            NextUrl(url, journeyData)
          }
    }

  private def onlyIfFeatureEnabled(f: => Future[Result]): Future[Result] =
    if (frontendConfig.showJourneySetup) {
      f
    } else Future.successful(NotImplemented("Feature not enabled."))

  private def withJourney(f: ASATestJourney => Future[Result])(implicit rh: RequestHeader): Future[Result] =
    rh.session
      .get("journey")
      .fold(Future.successful(Redirect((routes.AsaJourneySetupController.selectJourney()))))(journeyStr =>
        f(
          ASAJourneyService
            .asaJourneyForId(journeyStr)
            .get
        )
      )

  private def withJourneyData(f: String => Future[Result])(implicit rh: RequestHeader): Future[Result] =
    rh.session
      .get("journey-data")
      .fold(Future.successful(Redirect((routes.AsaJourneySetupController.selectJourney()))))(journeyDataStr =>
        f(journeyDataStr)
      )

}
