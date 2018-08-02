package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, session}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl

class SignInControllerISpec extends BaseISpec with AgentsExternalStubsStubs {

  private lazy val controller: SignInController = app.injector.instanceOf[SignInController]

  "SignInController" when {

    "GET /gg/sign-in" should {
      "display signIn page" in {
        val result = controller.showSignInPage(Some(ContinueUrl("/there")), "here", None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "POST /gg/sign-in" should {
      "redirect to continue URL if provided" in {
        val authToken = givenUserCanSignIn("foo", "bar", newUser = false)
        val result = controller.signIn(Some(ContinueUrl("/there")), "here", None)(FakeRequest()
          .withFormUrlEncodedBody("userId" -> "foo", "password" -> "bar"))
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.userId) shouldBe Some("foo")
        session(result).get(SessionKeys.sessionId) should not be empty
      }

      "redirect to edit user with if new one created" in {
        val authToken = givenUserCanSignIn("foo", "bar", newUser = true)
        val result = controller.signIn(Some(ContinueUrl("/there")), "here", None)(FakeRequest()
          .withFormUrlEncodedBody("userId" -> "foo", "password" -> "bar"))
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/user/edit?continue=%2Fthere")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.userId) shouldBe Some("foo")
        session(result).get(SessionKeys.sessionId) should not be empty
      }
    }

    "GET /gg/sign-out" should {
      "remove current session and redirect to the provided continue url" in {
        givenUserCanSignOut
        val result = controller.signOut(Some(ContinueUrl("/there")))(FakeRequest())
        status(result) shouldBe 303
      }
    }

  }

}
