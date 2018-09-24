package uk.gov.hmrc.agentsexternalstubsfrontend.controllers
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait WithPlanetId {

  def agentsExternalStubsConnector: AgentsExternalStubsConnector

  def withPlanetId(body: String => Future[Result])(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier,
    request: Request[_]): Future[Result] =
    request.session.get("planetId") match {
      case Some(planetId) => body(planetId)
      case None =>
        agentsExternalStubsConnector.currentSession
          .flatMap(session => body(session.planetId))
    }

}
