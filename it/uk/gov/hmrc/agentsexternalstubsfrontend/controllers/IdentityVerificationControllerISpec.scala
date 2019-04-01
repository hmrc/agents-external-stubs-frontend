package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{putRequestedFor, verify, urlEqualTo, containing}
import play.api.Application
import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class IdentityVerificationControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  trait Setup {
    val originalConfidenceLevel = 50
    givenAuthorised("Test123")
    givenUser(User(userId = "Test123", confidenceLevel = Some(originalConfidenceLevel)))
  }

  "IdentityVerificationController" when {

    "GET /mdtp/uplift" should {
      "display the identity verification uplift page" in new Setup {
        val result = callEndpointWith(FakeRequest(GET, "/mdtp/uplift?confidenceLevel=200&completionURL=/good&failureURL=/bad"))
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("uplift.header"))
      }

      "return 400 bad request" when {
        "the confidenceLevel query parameter is missing" in new Setup {
          val result = callEndpointWith(FakeRequest(GET, "/mdtp/uplift?completionURL=/good&failureURL=/bad"))
          status(result) shouldBe 400
        }

        "the completionUrl query parameter is missing" in new Setup {
          val result = callEndpointWith(FakeRequest(GET, "/mdtp/uplift?confidenceLevel=200&failureURL=/bad"))
          status(result) shouldBe 400
        }

        "the failureUrl query parameter is missing" in new Setup {
          val result = callEndpointWith(FakeRequest(GET, "/mdtp/uplift?confidenceLevel=200&completionURL=/good"))
          status(result) shouldBe 400
        }
      }
    }

    "POST /mdtp/uplift" when {
      "IV was chosen to succeed" should {
        val succeedRequest = addCsrfToken(FakeRequest(POST, "/mdtp/uplift?completionURL=/success&failureURL=/fail")
          .withFormUrlEncodedBody("confidenceLevel" -> "200", "willSucceed" -> "true"))

        "set the user's confidence level to the desired level" in new Setup {
          val result = callEndpointWith(succeedRequest)

          verify(1, putRequestedFor(urlEqualTo(s"/agents-external-stubs/users/Test123"))
            .withRequestBody(containing(""""confidenceLevel":200""")))
        }

        "redirect to the completionURL" in new Setup {
          val result = callEndpointWith(succeedRequest)

          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/success")
        }
      }

      "IV was chosen to fail" should {
        val failRequest = addCsrfToken(FakeRequest(POST, "/mdtp/uplift?completionURL=/success&failureURL=/fail")
          .withFormUrlEncodedBody("confidenceLevel" -> "200", "willSucceed" -> "false"))

        "leave the user's confidence level as it was" in new Setup {
          val result = callEndpointWith(failRequest)

          verify(0, putRequestedFor(urlEqualTo(s"/agents-external-stubs/users/Test123")))
        }

        "redirect to the failureURL" in new Setup {
          val result = callEndpointWith(failRequest)
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/fail")
        }
      }
    }
  }

  def addCsrfToken[T](fakeRequest: FakeRequest[T])(implicit app: Application) = {
    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token = csrfFilter.tokenProvider.generateToken

    fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag -> csrfConfig.tokenName,
      Token.RequestTag -> token
    )).withHeaders((csrfConfig.headerName, token))
  }
}
