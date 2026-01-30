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

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{JourneySetupInvitation, JourneySetupRequest}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentClientRelationshipsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.agentsexternalstubsfrontend.support.TestFixtures._
import uk.gov.hmrc.http.HeaderCarrier

class AgentClientRelationshipsConnectorISpec  extends BaseISpec with AgentClientRelationshipsStubs {

  private lazy val connector: AgentClientRelationshipsConnector = app.injector.instanceOf[AgentClientRelationshipsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "testOnlyJourneySetup" should {
    "return Future.unit" in {

      val req = JourneySetupRequest(
        Seq(JourneySetupInvitation(arn, nino, "ni", "", "HMRC-MTD-IT", None)))

      givenTestOnlyJourneySetup(req)
      val result: Unit = connector.testOnlyJourneySetup(req).futureValue

      result shouldBe ()
    }
  }

  "testOnlyCreateRelationship" should {
    "return Future.unit" in {

      givenTestOnlyCreateRelationship(arn, nino, "HMRC-MTD-IT", "personal")

      val result: Unit = connector.testOnlyCreateRelationship(arn, nino, "HMRC-MTD-IT", "ni").futureValue

      result shouldBe()
    }
  }
}
