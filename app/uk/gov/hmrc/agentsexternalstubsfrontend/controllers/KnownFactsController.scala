/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.EnrolmentKey
import uk.gov.hmrc.agentsexternalstubsfrontend.services.{Features, ServicesDefinitionsService}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.{error_template, show_all_services, show_known_facts}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KnownFactsController @Inject() (
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val servicesDefinitionsService: ServicesDefinitionsService,
  val features: Features,
  errorTemplateView: error_template,
  showKnownFactsView: show_known_facts,
  showAllServicesView: show_all_services,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  def showKnownFactsPage(enrolmentKey: String): Action[AnyContent] = Action.async { implicit request =>
    EnrolmentKey(enrolmentKey).fold(
      _ =>
        Future.successful(
          Ok(errorTemplateView("knownFacts.title", "Invalid request", "Provided enrolment key format is not valid."))
        ),
      _ =>
        authorised()
          .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
            agentsExternalStubsConnector
              .getKnownFacts(enrolmentKey)
              .map(enrolmentInfo =>
                Ok(
                  showKnownFactsView(
                    enrolmentInfo,
                    servicesDefinitionsService.servicesDefinitions
                      .getService(enrolmentInfo.enrolmentKey.service)
                      .getOrElse(throw new Exception()),
                    pageContext(credentials)
                  )
                )
              )
              .recover { case _: NotFoundException =>
                Ok(
                  errorTemplateView(
                    "knownFacts.title",
                    "Known facts not found",
                    "Known facts for the provided enrolment key does not exist on this test planet."
                  )
                )
              }
          }
    )
  }

  val showEnrolmentsPage: Action[AnyContent] = Action.async { implicit request =>
    if (features.showEnrolments)
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          Future.successful(
            Ok(
              showAllServicesView(servicesDefinitionsService.servicesDefinitions, pageContext(credentials))
            )
          )
        }
    else Future.successful(Forbidden)
  }

}
