package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class KnownFactsControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "KnownFactsController" when {

    "GET /agents-external-stubs/services" should {
      "render enrolments page" in {
        givenAuthorised()
        stubFor(
          get(urlEqualTo("/agents-external-stubs/config/services"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"services":[]}""".stripMargin)
            )
        )
        givenUser(User("Test123"))
        val request = FakeRequest(GET, "/agents-external-stubs/services")
        val result = callEndpointWith(request)
        status(result) shouldBe 200
      }
    }

  }

}
