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
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.GroupForm
import uk.gov.hmrc.agentsexternalstubsfrontend.services.{Features, ServicesDefinitionsService}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupController @Inject() (
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val servicesDefinitionsService: ServicesDefinitionsService,
  showGroupView: show_group,
  editGroupView: edit_group,
  showAllGroupsView: show_all_groups,
  val features: Features,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthorisedFunctions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  val start: Action[AnyContent] = showGroupPage(None, None)

  def showGroupPage(continue: Option[RedirectUrl], groupId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          for {
            group <- agentsExternalStubsConnector.getGroup(groupId.getOrElse(credentials.providerId))
            users <- agentsExternalStubsConnector.getUsersByGroupId(group.groupId)
          } yield Ok(
            showGroupView(
              group,
              users,
              request.session.get(SessionKeys.authToken),
              request.session.get(SessionKeys.sessionId),
              group.groupId,
              pageContext(credentials)
            )
          )
        }
    }

  def showEditGroupPage(continue: Option[RedirectUrl], groupId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector
            .getGroup(groupId.getOrElse(credentials.providerId))
            .map { group =>
              Ok(
                editGroupView(
                  GroupForm.form.fill(group),
                  routes.GroupController.updateGroup(continue, groupId),
                  routes.GroupController.showGroupPage(continue, groupId),
                  group.groupId,
                  continue.isDefined,
                  pageContext(credentials)
                )
              )
            }
        }
    }

  def updateGroup(continue: Option[RedirectUrl], groupId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          GroupForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    editGroupView(
                      formWithErrors,
                      routes.GroupController.updateGroup(continue, groupId),
                      routes.GroupController.showGroupPage(continue, groupId),
                      groupId = groupId.getOrElse(credentials.providerId),
                      continue.isDefined,
                      pageContext(credentials)
                    )
                  )
                ),
              group =>
                (agentsExternalStubsConnector
                  .updateGroup(group.copy(groupId = groupId.getOrElse(credentials.providerId))))
                  .map(_ =>
                    continue.fold(Redirect(routes.GroupController.showGroupPage(continue, groupId)))(continueUrl =>
                      Redirect(continueUrl.unsafeValue)
                    )
                  )
                  .recover { case e: Exception =>
                    Ok(
                      editGroupView(
                        GroupForm.form.fill(group).withGlobalError(e.getMessage),
                        routes.GroupController.updateGroup(continue, groupId),
                        routes.GroupController.showGroupPage(continue, groupId),
                        groupId = groupId.getOrElse(credentials.providerId),
                        continue.isDefined,
                        pageContext(credentials)
                      )
                    )
                  }
            )
        }
    }

  val showAllGroupsPage: Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector.getGroups
            .map(groups =>
              Ok(
                showAllGroupsView(
                  groups,
                  request.session.get(SessionKeys.authToken),
                  pageContext(credentials)
                )
              )
            )
        }
    }
}
