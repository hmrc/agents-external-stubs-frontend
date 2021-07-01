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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.PersonalDetailsValidationController.PdvRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.pdv_start
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationController @Inject() (
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  pdvStartView: pdv_start
)(implicit val configuration: Configuration, cc: MessagesControllerComponents, ec: ExecutionContext)
    extends FrontendController(cc) with AuthActions with I18nSupport {

  def start(completionUrl: RedirectUrl): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
      agentsExternalStubsConnector.getUser(credentials.providerId).map { currentUser =>
        Ok(
          pdvStartView(
            PersonalDetailsValidationController.PdvRequestForm.fill(PdvRequest(success = true)),
            routes.PersonalDetailsValidationController.submit(completionUrl)
          )
        )
      }
    }
  }

  def submit(completionUrl: RedirectUrl): Action[AnyContent] = Action.async { implicit request =>
    PersonalDetailsValidationController.PdvRequestForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            Ok(
              pdvStartView(
                formWithErrors,
                routes.PersonalDetailsValidationController.submit(completionUrl)
              )
            )
          ),
        pdvRequest => {
          val validationId = UUID.randomUUID().toString
          agentsExternalStubsConnector
            .storePdvResult(validationId, pdvRequest.success)
            .map(_ => Redirect(completionUrl.unsafeValue, Map("validationId" -> Seq(validationId))))
        }
      )
  }

}

object PersonalDetailsValidationController {

  case class PdvRequest(success: Boolean)

  object PdvRequest {
    implicit val format: OFormat[PdvRequest] = Json.format
  }

  val PdvRequestForm: Form[PdvRequest] = Form[PdvRequest](
    mapping(
      "success" -> boolean
    )(PdvRequest.apply)(PdvRequest.unapply)
  )
}
