package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class UserControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "UserController" when {

    "GET /agents-external-stubs/user" should {
      "display current user details" in {
        givenAuthorisedFor(
          """{"retrieve": ["credentials"]}""",
          s"""{
             |"credentials": {
             |    "providerId": "Test123",
             |    "providerType": "GovernmentGateway"
             |  }
             |}""".stripMargin
        )
        givenUser(User("Test123"))
        val request = FakeRequest(GET, "/agents-external-stubs/user")
        val result = callEndpointWith(request)
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("user.title"))
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Test123"))
      }
    }

  }

}
