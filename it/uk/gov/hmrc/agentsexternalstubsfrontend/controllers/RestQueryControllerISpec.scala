package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class RestQueryControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "RestQueryController" when {

    "GET /agents-external-stubs/rest-query" should {
      "render rest-query page" in {
        givenAuthorised()

        val request = FakeRequest(GET, "/agents-external-stubs/rest-query")

        val result = callEndpointWith(request)
        status(result) shouldBe 200
      }
    }

  }

}
