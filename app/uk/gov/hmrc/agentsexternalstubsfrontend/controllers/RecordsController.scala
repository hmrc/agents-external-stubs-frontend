package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class RecordsController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  val showAllRecordsPage: Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentials) { credentials =>
        agentsExternalStubsConnector.getRecords
          .map(
            records =>
              Ok(html.show_all_records(
                records,
                credentials.providerId,
                request.session.get(SessionKeys.authToken),
                routes.UserController.showUserPage(None),
                request.session.get("planetId").getOrElse("")
              )))
      }
  }

  def deleteRecord(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentials) { _ =>
        agentsExternalStubsConnector
          .deleteRecord(id)
          .map(_ => Redirect(routes.RecordsController.showAllRecordsPage()))
      }
  }

}

object RecordsController {}
