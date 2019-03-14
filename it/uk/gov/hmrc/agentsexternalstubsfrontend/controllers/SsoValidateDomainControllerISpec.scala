package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class SsoValidateDomainControllerISpec extends BaseISpec {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "PlanetController" when {

    "GET /sso/validate/domain/localhost" should {
      "return NoContent" in {
        val request = FakeRequest(GET, "/sso/validate/domain/localhost")
        val result = callEndpointWith(request)
        status(result) shouldBe 204
      }
    }

  }

}
