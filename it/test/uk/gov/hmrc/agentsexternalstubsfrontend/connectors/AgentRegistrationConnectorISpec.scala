/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class AgentRegistrationConnectorISpec extends BaseISpec with AgentRegistrationStubs {

  private lazy val connector: AgentRegistrationConnector = app.injector.instanceOf[AgentRegistrationConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "testOnlyJourneySetup" should {
    "return Future.unit" in {
      givenLinkCreated()
      val result = connector.testOnlyJourneySetup().futureValue
      result.linkId shouldBe "abc123"
    }
  }
}
