package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.Application
import play.api.http.Writeable
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Headers, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class IdentityVerificationControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  trait Setup {
    val originalConfidenceLevel = 50
    givenAuthorised("Test123")
    givenUser(User(userId = "Test123", confidenceLevel = Some(originalConfidenceLevel), nino = Some(Nino("AB626225C"))))
  }

  "IdentityVerificationController" when {

    "GET /mdtp/uplift" should {
      "display the identity verification uplift page" in new Setup {

        val result =
          callEndpointWith(
            FakeRequest(GET, "/mdtp/uplift?confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=aif")
          )
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
        val succeedRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "Success~1234")
        )

        "redirect to the completionURL" in new Setup {
          val result = callEndpointWith(succeedRequest)

          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/good?journeyId=1234")
        }
      }

      "IV was chosen to fail" should {
        val failRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "PreconditionFailed~1234")
        )

        "redirect to the failureURL" in new Setup {

          val result = callEndpointWith(failRequest)
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/bad?journeyId=1234")
        }
      }
    }

    "GET /agents-external-stubs/mdtp/uplift" should {
      "display the identity verification uplift page" in new Setup {

        val result = callEndpointWith(
          FakeRequest(GET, "/agents-external-stubs/mdtp/uplift?confidenceLevel=200&completionURL=/good&failureURL=/bad")
        )
        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("uplift.header"))
      }

      "return 400 bad request" when {
        "the confidenceLevel query parameter is missing" in new Setup {

          val result =
            callEndpointWith(FakeRequest(GET, "/agents-external-stubs/mdtp/uplift?completionURL=/good&failureURL=/bad"))
          status(result) shouldBe 400
        }

        "the completionUrl query parameter is missing" in new Setup {

          val result =
            callEndpointWith(FakeRequest(GET, "/agents-external-stubs/mdtp/uplift?confidenceLevel=200&failureURL=/bad"))
          status(result) shouldBe 400
        }

        "the failureUrl query parameter is missing" in new Setup {

          val result = callEndpointWith(
            FakeRequest(GET, "/agents-external-stubs/mdtp/uplift?confidenceLevel=200&completionURL=/good")
          )
          status(result) shouldBe 400
        }
      }
    }

    "POST /agents-external-stubs/mdtp/uplift" when {
      "IV was chosen to succeed" should {
        val succeedRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/agents-external-stubs/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "Success~1234")
        )

        "redirect to the completionURL" in new Setup {

          val result = callEndpointWith(succeedRequest)

          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/good?journeyId=1234")
        }
      }

      "IV was chosen to fail" should {
        val failRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/agents-external-stubs/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "PreconditionFailed~1234")
        )

        "redirect to the failureURL" in new Setup {
          val result = callEndpointWith(failRequest)
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some("/bad?journeyId=1234")
        }
      }
    }

    "GET /mdtp/journey/journeyId/:journeyId" should {

      "return 200 with Json" in new Setup {

        val failRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/agents-external-stubs/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "PreconditionFailed~1234")
        )

        callEndpointWith(failRequest)

        val fakeRequest = FakeRequest(GET, "/mdtp/journey/journeyId/1234")

        val result = callEndpointWith(fakeRequest)
        status(result) shouldBe 200
        (contentAsJson(Future.successful(result)) \ "result").as[String] shouldBe "PreconditionFailed"
      }

      "return 404 with no Json" in new Setup {
        val failedFakeRequest = FakeRequest(GET, "/mdtp/journey/journeyId/1234")

        val result = callEndpointWith(failedFakeRequest)
        status(result) shouldBe 404
      }
    }

    "GET /agents-external-stubs/mdtp/journey/journeyId/:journeyId" when {

      "return 200 with Json" in new Setup {

        val failRequest = addCsrfToken(
          FakeRequest(
            POST,
            "/agents-external-stubs/mdtp/uplift?journeyId=1234&confidenceLevel=200&completionURL=/good&failureURL=/bad&origin=ai"
          )
            .withFormUrlEncodedBody("nino" -> "AB626225C", "option" -> "PreconditionFailed~1234")
        )

        callEndpointWith(failRequest)

        val fakeRequest = FakeRequest(GET, "/agents-external-stubs/mdtp/journey/journeyId/1234")

        val result = callEndpointWith(fakeRequest)
        status(result) shouldBe 200
        (contentAsJson(Future.successful(result)) \ "result").as[String] shouldBe "PreconditionFailed"
      }

      "return 404 with no Json" in new Setup {
        val failedFakeRequest = FakeRequest(GET, "/agents-external-stubs/mdtp/journey/journeyId/1234")

        val result = callEndpointWith(failedFakeRequest)
        status(result) shouldBe 404
      }
    }
  }

  def addCsrfToken[T](fakeRequest: FakeRequest[T])(implicit app: Application) = {
    val csrfConfig = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter = app.injector.instanceOf[CSRFFilter]
    val token = csrfFilter.tokenProvider.generateToken

    fakeRequest
      .addAttr(TypedKey("tokenName"), csrfConfig.tokenName)
      .withHeaders(Headers(csrfConfig.headerName -> token))
  }
}
