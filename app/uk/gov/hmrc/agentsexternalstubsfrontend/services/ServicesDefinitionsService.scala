/*
 * Copyright 2020 HM Revenue & Customs
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
