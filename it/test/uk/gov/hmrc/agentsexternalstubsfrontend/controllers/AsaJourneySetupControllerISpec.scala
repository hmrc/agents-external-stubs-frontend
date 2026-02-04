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

package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, await, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{AuthProvider, JourneySetupInvitation, JourneySetupRequest, User}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentClientRelationshipsStubs, AgentsExternalStubsStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.agentsexternalstubsfrontend.support.TestFixtures._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class AsaJourneySetupControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AgentClientRelationshipsStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  val asaDoorwayPath: String      = "/agents-external-stubs/asa-doorway"
  val selectJourneyPath: String   = s"$asaDoorwayPath/select-journey"
  val selectServicePath: String   = s"$asaDoorwayPath/select-service"
  val journeyDataPath: String     = s"$asaDoorwayPath/journey-data"

  s"GET $asaDoorwayPath" should {
    s"redirect to $selectJourneyPath" in {
      val result = callEndpointWith(FakeRequest(GET, asaDoorwayPath))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe selectJourneyPath
    }
  }

  s"GET $selectJourneyPath" should {
    "return OK when no journey query parameter is supplied" in {

      val result = callEndpointWith(FakeRequest(GET, selectJourneyPath))
      status(result) shouldBe OK
    }

    s"redirect to $selectServicePath when a valid query parameter is supplied" in {

      givenSignIn("bob", "authToken", "GovernmentGateway", "mars")
      givenUserCanSignIn("agent1", "mars")
      givenUser(User(userId = "bob"))
      givenCreateUser(Agent, List(asaEnrolment), "agent2")

      val result = callEndpointWith(FakeRequest(GET, selectJourneyPath + "?journey=create-invitation"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe selectServicePath
    }

    s"redirect to $selectServicePath when hmrc-led-deauth query parameter is supplied" in {

      givenSignIn("bob", "authToken", AuthProvider.PrivilegedApplication, "mars")
      givenUserCanSignIn("stride1", "mars", providerType = AuthProvider.PrivilegedApplication)
      givenUser(User(userId = "bob"))
      givenCreateStrideUser("maintain_agent_relationships", "stride1")

      val result = callEndpointWith(FakeRequest(GET, selectJourneyPath + "?journey=hmrc-led-deauth"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe selectServicePath
    }

    s"return BadRequest when invalid query parameter is supplied" in {

      val result = callEndpointWith(FakeRequest(GET, selectJourneyPath + "?journey=invalid"))

      status(result) shouldBe BAD_REQUEST
    }
  }

  s"GET $selectServicePath" should {
    s"redirect to $selectJourneyPath when no journey is saved to session" in {

      val result = callEndpointWith(FakeRequest(GET, selectServicePath))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe selectJourneyPath
    }

    s"return OK when the journey saved is a ASATestJourneyWithServiceSelection" in {

      val result = callEndpointWith(FakeRequest(GET, selectServicePath).withSession("journey" -> "create-invitation"))

      status(result) shouldBe OK
    }

    s"redirect to handoff url when the journey saved is MYTA (individual)" in {

      givenUser(agentOrClientUser("bob", List(itsaEnrolment, ptEnrolment)))
      givenCreateUser(Agent, List(asaEnrolment), "agent1")
      givenTestOnlyJourneySetup(JourneySetupRequest(Seq(
        JourneySetupInvitation(arn, nino, "ni", "", "HMRC-MTD-IT", Some("personal")),
        JourneySetupInvitation(arn, nino, "ni", "", "PERSONAL-INCOME-RECORD", Some("personal")),
        JourneySetupInvitation(arn, nino, "ni", "", "HMRC-MTD-IT-SUPP", Some("personal")),
        JourneySetupInvitation(arn, nino, "ni", "", "PERSONAL-INCOME-RECORD", Some("personal"))
      )))

      val result = callEndpointWith(FakeRequest(GET, selectServicePath)
        .withSession("journey" -> "myta-ind", "userId" -> "bob"))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe "http://localhost:9435/agent-client-relationships/test-only/journey-setup/myta"
    }

    s"redirect to $journeyDataPath when the journey saved is Uk Subscription" in {

      givenUser(agentOrClientUser("agent1", List(hmceVatAgntEnrolment)))
      givenCreateCleanAgent("agent2")
      givenGetRecord("123", s"""{"utr": "12345", "crn": "a876", "addressDetails": {"postalCode": "BN3111"}}""")


      val result = callEndpointWith(FakeRequest(GET, selectServicePath)
        .withSession("journey" -> "uk-subscription", "userId" -> "bob"))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe s"$journeyDataPath"
    }
  }

  s"POST $selectServicePath" should {
    s"redirect to $journeyDataPath" in {

      givenUser(agentOrClientUser("agent1", List(asaEnrolment)))
      givenCreateUser(Individual, List(itsaEnrolment), "client")

      givenTestOnlyJourneySetup(JourneySetupRequest(Seq(JourneySetupInvitation(arn, nino, "ni", "", "HMRC-MTD-IT", Some("personal")))))

      val result = callEndpointWith(FakeRequest(POST, selectServicePath).withFormUrlEncodedBody(("service", "Itsa"))
        .withSession("journey" -> "create-invitation", "userId" -> "agent1"))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe s"$journeyDataPath"
    }

    s"redirect to $selectJourneyPath when wrong type of journey saved in session" in {

      val result = callEndpointWith(FakeRequest(POST, selectServicePath).withFormUrlEncodedBody(("service", "Itsa"))
        .withSession("journey" -> "uk-subscription", "userId" -> "agent1"))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe s"$selectJourneyPath"
    }
  }

  s"GET $journeyDataPath" should {
    "display the test data" in {

      val result = callEndpointWith(FakeRequest(GET, journeyDataPath)
        .withSession("journey" -> "create-invitation", "userId" -> "agent1", "journey-data" -> s"""{"nino": "$nino", "postcode": "BN1"} """))

      status(result) shouldBe OK

    }
  }

}
