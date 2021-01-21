/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlanetController @Inject() (
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions {

  implicit val ec: ExecutionContext = ecp.get

  val destroyPlanet: Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector
          .destroyPlanet(credentials.planetId)
          .map(_ => Redirect(routes.UserController.start()).withNewSession)
      }
  }

}
