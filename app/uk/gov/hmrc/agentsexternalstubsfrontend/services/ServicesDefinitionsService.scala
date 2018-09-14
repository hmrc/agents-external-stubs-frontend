package uk.gov.hmrc.agentsexternalstubsfrontend.services
import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.Services
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class ServicesDefinitionsService @Inject()(
  agentsExternalStubsConnector: AgentsExternalStubsConnector,
  materializer: Materializer) {

  lazy val servicesDefinitions: Services = Await
    .result(agentsExternalStubsConnector.getServicesInfo()(HeaderCarrier(), materializer.executionContext), 30.seconds)
}
