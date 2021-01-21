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
import play.api.data.Forms.{mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, SpecialCase}
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.{edit_special_case, show_all_special_cases}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SpecialCasesController @Inject() (
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  showAllSpecialCasesView: show_all_special_cases,
  editSpecialCaseView: edit_special_case,
  val features: Features,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  import SpecialCasesController._

  def showAllSpecialCasesPage(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector.getAllSpecialCases
          .map(specialCases =>
            Ok(
              showAllSpecialCasesView(
                specialCases,
                caseId,
                id => routes.SpecialCasesController.showAllSpecialCasesPage(Some(id)),
                id => routes.SpecialCasesController.showEditSpecialCasePage(Some(id)),
                routes.SpecialCasesController.deleteSpecialCase,
                pageContext(credentials)
              )
            )
          )
      }
  }

  def showEditSpecialCasePage(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        caseId match {
          case Some(id) =>
            agentsExternalStubsConnector
              .getSpecialCase(id)
              .map {
                case Some(specialCase) =>
                  Ok(
                    editSpecialCaseView(
                      SpecialCaseForm.fill(specialCase),
                      caseId,
                      routes.SpecialCasesController.upsertSpecialCase(caseId),
                      routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                      routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                      pageContext(credentials)
                    )
                  )
                case None =>
                  Ok(
                    editSpecialCaseView(
                      SpecialCaseForm.withGlobalError(s"Special Case with id=$id has not been found."),
                      caseId,
                      routes.SpecialCasesController.upsertSpecialCase(None),
                      routes.SpecialCasesController.showEditSpecialCasePage(None),
                      routes.SpecialCasesController.showAllSpecialCasesPage(None),
                      pageContext(credentials)
                    )
                  )
              }
          case None =>
            Future.successful(
              Ok(
                editSpecialCaseView(
                  SpecialCaseForm,
                  None,
                  routes.SpecialCasesController.upsertSpecialCase(caseId),
                  routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                  routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                  pageContext(credentials)
                )
              )
            )
        }
      }
  }

  def upsertSpecialCase(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        SpecialCaseForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                Ok(
                  editSpecialCaseView(
                    formWithErrors,
                    caseId,
                    routes.SpecialCasesController.upsertSpecialCase(caseId),
                    routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                    routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                    pageContext(credentials)
                  )
                )
              ),
            specialCase =>
              (caseId match {
                case Some(_) =>
                  agentsExternalStubsConnector.updateSpecialCase(specialCase.copy(id = caseId))
                case None =>
                  agentsExternalStubsConnector.createSpecialCase(specialCase)
              }).map(id => Redirect(routes.SpecialCasesController.showAllSpecialCasesPage(Some(id))))
          )
      }
  }

  def deleteSpecialCase(caseId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { _ =>
        agentsExternalStubsConnector
          .deleteSpecialCase(caseId)
          .map(_ => Redirect(routes.SpecialCasesController.showAllSpecialCasesPage(None)))
      }
  }

}

object SpecialCasesController {

  val requestMatchMapping: Mapping[SpecialCase.RequestMatch] = mapping(
    "path" -> nonEmptyText
      .transform[String](path => if (!path.startsWith("/")) "/" + path else path, identity)
      .verifying(path => path != "/")
      .verifying(path => !(path.startsWith("/agents-external-stubs") || path.startsWith("/ping"))),
    "method"      -> nonEmptyText,
    "body"        -> optional(nonEmptyText),
    "contentType" -> optional(nonEmptyText)
  )(SpecialCase.RequestMatch.apply)(SpecialCase.RequestMatch.unapply)

  val headerMapping: Mapping[SpecialCase.Header] = mapping(
    "name"  -> nonEmptyText,
    "value" -> nonEmptyText
  )(SpecialCase.Header.apply)(SpecialCase.Header.unapply)

  val responseMapping = mapping(
    "status" -> number(200, 599),
    "body"   -> optional(nonEmptyText),
    "headers" -> optional(seq(optional(headerMapping)))
      .transform[Option[Seq[SpecialCase.Header]]](_.map(_.collect { case Some(x) => x }), _.map(_.map(Option.apply)))
  )(SpecialCase.Response.apply)(SpecialCase.Response.unapply)

  val SpecialCaseForm: Form[SpecialCase] = Form[SpecialCase](
    mapping(
      "requestMatch" -> requestMatchMapping,
      "response"     -> responseMapping,
      "id"           -> optional(text)
    )(SpecialCase.apply)(SpecialCase.unapply)
  )

}
