package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class PlanetControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "PlanetController" when {

    "DELETE /agents-external-stubs/planet/destroy" should {
      "destroy existing test planet and redirect to the start page" ignore {
        givenAuthorisedFor(
          """{"retrieve": []}""",
          s"""{}""".stripMargin
        )
        givenUser(User("Test123"))
        val request = FakeRequest(GET, "/agents-external-stubs/planet/destroy")
          .withSession("planetId" -> "foo")
        val result = callEndpointWith(request)
        status(result) shouldBe 304
        redirectLocation(result) shouldBe Some("/agents-external-stubs/start")
      }
    }

  }

}
