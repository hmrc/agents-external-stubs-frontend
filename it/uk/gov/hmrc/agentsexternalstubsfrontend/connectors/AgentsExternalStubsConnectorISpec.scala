package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AgentsExternalStubsConnectorISpec extends BaseISpec with AgentsExternalStubsStubs {
  private implicit val hc = HeaderCarrier()

  private lazy val connector: AgentsExternalStubsConnector = new AgentsExternalStubsConnector(
    new URL(s"http://localhost:$wireMockPort"),
    app.injector.instanceOf[HttpGet with HttpPost])

  "AgentsExternalStubsConnector" when {

    "signIn" should {

      "return authenticated session with new user flag on" in {
        val sessionId = givenUserCanSignIn("foo", "bar")
        val response: AuthenticatedSession = await(connector.signIn(SignInRequest("foo", "bar")))
        response.authToken shouldBe sessionId
        response.userId shouldBe "foo"
        response.providerType shouldBe "GovernmentGateway"
        response.newUserCreated shouldBe Some(true)
      }

      "return authenticated session with new user flag off" in {
        val sessionId = givenUserCanSignIn("foo", "bar", newUser = false)
        val response: AuthenticatedSession = await(connector.signIn(SignInRequest("foo", "bar")))
        response.authToken shouldBe sessionId
        response.userId shouldBe "foo"
        response.providerType shouldBe "GovernmentGateway"
        response.newUserCreated shouldBe Some(false)
      }

      "throw an exception if no connection was possible" in {
        stopWireMockServer()
        intercept[BadGatewayException] {
          await(connector.signIn(SignInRequest("foo", "bar")))
        }
        startWireMockServer()
      }

      "throw an exception if the response is 400" in {
        stubFor(
          post(urlEqualTo(s"/agents-external-stubs/sign-in"))
            .willReturn(aResponse()
              .withStatus(Status.BAD_REQUEST)))

        intercept[BadRequestException] {
          await(connector.signIn(SignInRequest("foo", "bar")))
        }
      }
    }
  }

}
