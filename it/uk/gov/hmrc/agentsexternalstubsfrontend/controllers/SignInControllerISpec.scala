package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{cookies, redirectLocation, session}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.binders.ContinueUrl
import play.api.mvc.Cookie

class SignInControllerISpec extends BaseISpec with AgentsExternalStubsStubs {

  private lazy val controller: SignInController = app.injector.instanceOf[SignInController]

  "SignInController" when {

    "GET /gg/sign-in" should {
      "display signIn page" in {
        val result = controller.showSignInPage(Some(ContinueUrl("/there")), Some("here"), None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /bas-gateway/sign-in" should {
      "display signIn page" in {
        val result = controller.showSignInPageSCP(Some(ContinueUrl("/there")), None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /bas-gateway/register" should {
      "display signIn page" in {
        val result = controller.register(Some(ContinueUrl("/there")), None, None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /government-gateway-registration-frontend" should {
      "display the sign in page" in {
        val result =
          controller.showGovernmentGatewaySignInPage(Some(ContinueUrl("/there")), Some("unknown"), Some("agent"))(
            FakeRequest()
          )
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /agents-external-stubs/gg/sign-in" should {
      "display signIn page" in {
        val result = controller.showSignInPageInternal(Some(ContinueUrl("/there")), Some("here"), None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /agents-external-stubs/bas-gateway/sign-in" should {
      "display signIn page" in {
        val result = controller.showSignInPageInternalSCP(Some(ContinueUrl("/there")), None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /agents-external-stubs/bas-gateway/register" should {
      "display signIn page" in {
        val result = controller.registerInternal(Some(ContinueUrl("/there")), None, None)(FakeRequest())
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "GET /agents-external-stubs/government-gateway-registration-frontend" should {
      "display the sign in page" in {
        val result =
          controller.showGovernmentGatewaySignInPageInternal(Some(ContinueUrl("/there")), Some("here"), None)(
            FakeRequest()
          )
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("start.title"))
      }
    }

    "POST /gg/sign-in" should {
      "redirect to continue URL if provided" in {
        val authToken = givenUserCanSignIn("foo", "juniper", newUser = false)
        val result = controller.signIn(Some(ContinueUrl("/there")), None, None, AuthProvider.GovernmentGateway)(
          FakeRequest()
            .withFormUrlEncodedBody("userId" -> "foo", "planetId" -> "juniper")
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.sessionId) should not be empty
      }

      "redirect to edit user with if new one created" in {
        val authToken = givenUserCanSignIn("foo", "saturn", newUser = true)
        val result = controller.signIn(Some(ContinueUrl("/there")), None, None, AuthProvider.GovernmentGateway)(
          FakeRequest()
            .withFormUrlEncodedBody("userId" -> "foo", "planetId" -> "saturn")
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/user/create?continue=%2Fthere")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.sessionId) should not be empty
      }
    }

    "GET /bas-gateway/sso-sign-in" should {
      "display signIn page if no session and continue url" in {
        givenNoCurrentSessionExist()
        val result = controller.signInSsoSCP(Some(ContinueUrl("/there")), None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/bas-gateway/sign-in?continue_url=%2Fthere")
      }

      "display signIn page if no session and no continue url" in {
        givenNoCurrentSessionExist()
        val result = controller.signInSsoSCP(None, None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/bas-gateway/sign-in")
      }

      "display internal signIn page if no session and continue url" in {
        givenNoCurrentSessionExist()
        val result = controller.signInSsoInternalSCP(Some(ContinueUrl("/there")), None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/bas-gateway/sign-in?continue_url=%2Fthere")
      }

      "display internal signIn page if no session and no continue url" in {
        givenNoCurrentSessionExist()
        val result = controller.signInSsoInternalSCP(None, None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/bas-gateway/sign-in")
      }

      "redirect to continue url if session exist" in {
        givenCurrentSession()
        val result = controller.signInSsoSCP(Some(ContinueUrl("/there")), None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
      }

      "redirect to the user page if session exist but no continue url" in {
        givenCurrentSession()
        val result = controller.signInSsoSCP(None, None, None)(FakeRequest())
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/user")
      }
    }

    "POST /agents-external-stubs/gg/sign-in" should {
      "redirect to continue URL if provided" in {
        val authToken = givenUserCanSignIn("foo", "juniper", newUser = false)
        val result = controller.signInInternal(Some(ContinueUrl("/there")), None, None, AuthProvider.GovernmentGateway)(
          FakeRequest()
            .withFormUrlEncodedBody("userId" -> "foo", "planetId" -> "juniper")
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.sessionId) should not be empty
      }

      "redirect to edit user with if new one created" in {
        val authToken = givenUserCanSignIn("foo", "saturn", newUser = true)
        val result = controller.signInInternal(Some(ContinueUrl("/there")), None, None, AuthProvider.GovernmentGateway)(
          FakeRequest()
            .withFormUrlEncodedBody("userId" -> "foo", "planetId" -> "saturn")
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/user/create?continue=%2Fthere")
        session(result).get(SessionKeys.authToken) shouldBe Some(s"Bearer $authToken")
        session(result).get(SessionKeys.sessionId) should not be empty
      }
    }

    "GET /gg/sign-out" should {
      "remove current session and redirect to the provided continue url" in {
        givenUserCanSignOut
        val result = controller.signOut(Some(ContinueUrl("/there")))(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }

      "remove current session and redirect to the sign in page" in {
        givenUserCanSignOut
        val result = controller.signOut(None)(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/gg/sign-in")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }
    }

    "GET /bas-gateway/sign-out-without-state" should {
      "remove current session and redirect to the provided continue url" in {
        givenUserCanSignOut
        val result = controller.signOutSCP(Some(ContinueUrl("/there")))(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }

      "remove current session and redirect to the sign in page" in {
        givenUserCanSignOut
        val result = controller.signOutSCP(None)(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/bas-gateway/sign-in")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }
    }

    "GET /agents-external-stubs/gg/sign-out" should {
      "remove current session and redirect to the provided continue url" in {
        givenUserCanSignOut
        val result = controller.signOutInternal(Some(ContinueUrl("/there")))(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }

      "remove current session and redirect to the sign in page" in {
        givenUserCanSignOut
        val result = controller.signOutInternal(None)(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/gg/sign-in")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }
    }

    "GET /agents-external-stubs/bas-gateway/sign-out-without-state" should {
      "remove current session and redirect to the provided continue url" in {
        givenUserCanSignOut
        val result = controller.signOutInternalSCP(Some(ContinueUrl("/there")))(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/there")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }

      "remove current session and redirect to the sign in page" in {
        givenUserCanSignOut
        val result = controller.signOutInternalSCP(None)(
          FakeRequest()
            .withSession("foo" -> "bar")
            .withCookies(Cookie("FOO", "BAR"))
        )
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs/bas-gateway/sign-in")
        cookies(result).get("PLAY_SESSION") shouldBe None
        cookies(result).get("FOO") shouldBe None
      }
    }

  }

}
