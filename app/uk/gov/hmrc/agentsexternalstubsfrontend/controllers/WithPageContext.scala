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

  val item1 = MenuItem("link_records_all", "records.link.all", routes.RecordsController.showAllRecordsPage())
  val item2 = MenuItem("link_users_all", "users.link.all", routes.UserController.showAllUsersPage())
  val item3 = MenuItem("link_users_current", "users.link.current", routes.UserController.showUserPage())
  val item4 = MenuItem("link_help_enrolments", "services.link.short", routes.KnownFactsController.showEnrolmentsPage())
  val item7 =
    MenuItem("link_special_cases", "specialCase.link.short", routes.SpecialCasesController.showAllSpecialCasesPage())
  val item5 =
    MenuItem("link_rest_query", "rest.query.link.short", routes.RestQueryController.showRestQueryPage())
  val item6 = MenuItem("link_sign_out", "common.sign-out", routes.SignInController.signOutInternal())

  val menu1 = Seq(item1, item2, item3, item7, item6)
  val menu2 = Seq(item1, item2, item3, item4, item7, item6)
  val menu3 = Seq(item1, item2, item3, item7, item5, item6)
  val menu4 = Seq(item1, item2, item3, item4, item7, item5, item6)

}
