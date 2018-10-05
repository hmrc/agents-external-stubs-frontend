package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class HelpController @Inject()(
  override val messagesApi: MessagesApi
)(implicit val configuration: Configuration)
    extends FrontendController with I18nSupport {

  def showHelpPage(name: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(name).map {
      case "agent-authorisation-api" => Ok(html.help.help_agent_authorisation_api())
      case _                         => BadRequest("Help page not found.")
    }
  }

}
