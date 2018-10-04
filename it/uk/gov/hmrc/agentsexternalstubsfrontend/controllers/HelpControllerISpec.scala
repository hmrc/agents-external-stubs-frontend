package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class HelpControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "HelpController" when {

    "GET /agents-external-stubs/help/agent-authorisation-api" should {
      "render help page" in {
        val request = FakeRequest(GET, "/agents-external-stubs/help/agent-authorisation-api")
        val result = callEndpointWith(request)
        status(result) shouldBe 200
      }
    }

    "GET /agents-external-stubs/help/foobar" should {
      "return 400 Bad Request" in {
        val request = FakeRequest(GET, "/agents-external-stubs/help/foobar")
        val result = callEndpointWith(request)
        status(result) shouldBe 400
      }
    }

  }

}
