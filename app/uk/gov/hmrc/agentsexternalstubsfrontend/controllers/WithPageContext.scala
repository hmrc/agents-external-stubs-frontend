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
      Seq(
        MenuItem("link_records_all", "records.link.all", routes.RecordsController.showAllRecordsPage()),
        MenuItem("link_users_all", "users.link.all", routes.UserController.showAllUsersPage()),
        MenuItem("link_users_current", "users.link.current", routes.UserController.showUserPage())
      ) ++ (if (features.showEnrolments)
              Seq(
                MenuItem(
                  "link_help_enrolments",
                  "services.link.short",
                  routes.KnownFactsController.showEnrolmentsPage()))
            else Seq.empty) ++ (if (features.mayShowRestQuery(credentials.planetId))
                                  Seq(
                                    MenuItem(
                                      "link_rest_query",
                                      "rest.query.link.short",
                                      routes.RestQueryController.showRestQueryPage()))
                                else Seq.empty) ++
        Seq(MenuItem("link_sign_out", "common.sign-out", routes.SignInController.signOut()))

    override def features: Features = self.features
  }

}
