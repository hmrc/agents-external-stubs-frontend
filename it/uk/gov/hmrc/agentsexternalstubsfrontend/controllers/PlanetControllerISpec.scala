package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, stubFor, urlEqualTo}
import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class PlanetControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "PlanetController" when {

    "GET /agents-external-stubs/planet/destroy" should {
      "destroy existing test planet and redirect to the start page" in {
        givenAuthorised("Test123")
        stubFor(
          delete(urlEqualTo("/agents-external-stubs/planets/foobar"))
            .willReturn(aResponse()
              .withStatus(204)))

        val request = FakeRequest(GET, "/agents-external-stubs/planet/destroy")

        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs")
      }
    }

  }

}
