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
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{CredentialsWithPlanetId, MenuItem, PageContext}
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features

trait WithPageContext {
  self =>

  def features: Features

  def pageContext(credentials: CredentialsWithPlanetId): PageContext = new PageContext {

    override def authorisedUserId: String = credentials.providerId

    override def planetId: String = credentials.planetId

    override def menuItems: Seq[MenuItem] =
      (features.showEnrolments, features.mayShowRestQuery(credentials.planetId)) match {
        case (true, true)   => Menus.menu4
        case (false, true)  => Menus.menu3
        case (true, false)  => Menus.menu2
        case (false, false) => Menus.menu1
      }

    override def features: Features = self.features
  }

}

object Menus {

  val quickStartHub = MenuItem("link_quick_start", "ASA links", routes.SignInController.showQuickStart())
  val records = MenuItem("link_records_all", "records.link.all", routes.RecordsController.showAllRecordsPage())
  val users = MenuItem("link_users_all", "users.link.all", routes.UserController.showAllUsersPage)
  val groups = MenuItem("link_groups_all", "groups.link.all", routes.GroupController.showAllGroupsPage)
  val showCurrentUser = MenuItem("link_users_current", "users.link.current", routes.UserController.showUserPage())
  val enrolments =
    MenuItem("link_help_enrolments", "services.link.short", routes.KnownFactsController.showEnrolmentsPage)
  val specialCases =
    MenuItem("link_special_cases", "specialCase.link.short", routes.SpecialCasesController.showAllSpecialCasesPage())
  val restQuery =
    MenuItem("link_rest_query", "rest.query.link.short", routes.RestQueryController.showRestQueryPage())

  val menu1 = Seq(quickStartHub, records, users, groups, showCurrentUser, specialCases)
  val menu2 = Seq(quickStartHub, records, users, groups, showCurrentUser, enrolments, specialCases)
  val menu3 = Seq(quickStartHub, records, users, groups, showCurrentUser, specialCases, restQuery)
  val menu4 = Seq(quickStartHub, records, users, groups, showCurrentUser, enrolments, specialCases, restQuery)

}
