package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{User, Users}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AgentsExternalStubsConnectorISpec extends BaseISpec with AgentsExternalStubsStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector: AgentsExternalStubsConnector = new AgentsExternalStubsConnector(
    new URL(s"http://localhost:$wireMockPort"),
    app.injector.instanceOf[HttpGet with HttpPost with HttpPut with HttpDelete])

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

    "getUser" should {
      "return an user for a valid userId" in {
        givenUser(User("foo"))
        val user: User = await(connector.getUser("foo"))
        user.userId shouldBe "foo"
      }
    }

    "getUsers" should {
      "return an user for a valid userId" in {
        givenUsers(User("foo"), User("bar"))
        val users: Users = await(connector.getUsers)
        users.users.map(_.userId) should contain.only("foo", "bar")
      }
    }

    "updateUser" should {
      "update an user" in {
        val user = User("foo")
        givenUser(user)
        await(connector.updateUser(user))
      }
    }

    "removeUser" should {
      "remove an user" in {
        val user = User("foo")
        givenUser(user)
        await(connector.removeUser(user.userId))
      }
    }

    "signOut" should {
      "remove authenticated session" in {
        givenUserCanSignOut
        await(connector.signOut())
      }
    }

    "getRecords" should {
      "get a map of all master records" in {
        givenAllRecords
        val result = await(connector.getRecords)
        result.VatCustomerInformationRecord should not be empty
      }
    }
  }

}
