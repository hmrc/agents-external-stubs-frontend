package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class PlanetController @Inject()(
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions {

  val destroyPlanet: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      request.session.get("planetId") match {
        case Some(planetId) =>
          agentsExternalStubsConnector
            .destroyPlanet(planetId)
            .map(_ => Redirect(routes.UserController.start()).withNewSession)
        case None =>
          Future.successful(BadRequest("Unknown planet."))
      }
    }
  }

}
