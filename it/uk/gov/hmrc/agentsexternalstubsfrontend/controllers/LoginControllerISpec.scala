package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, session}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl

class LoginControllerISpec extends BaseISpec with AgentsExternalStubsStubs {

  private lazy val controller: LoginController = app.injector.instanceOf[LoginController]

  "LoginController" when {

    "GET /gg/sign-in" should {
      "display login page" in {
        val result = controller.showLogin(Some(ContinueUrl("/there")), "here", None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "POST /gg/sign-in" should {
      "redirect to continue URL if provided" in {
        val sessionId = givenUserCanSignIn("foo", "bar")
        val result = controller.login(Some(ContinueUrl("/there")), "here", None)(FakeRequest()
          .withFormUrlEncodedBody("userId" -> "foo", "password" -> "bar"))
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        session(result).get(SessionKeys.sessionId) shouldBe Some(sessionId)
      }
    }

  }

}
