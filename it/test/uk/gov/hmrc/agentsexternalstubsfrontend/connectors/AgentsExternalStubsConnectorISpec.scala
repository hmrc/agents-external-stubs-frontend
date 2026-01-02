/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import play.api.http.Status
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.SpecialCase.RequestMatch
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{AuthProvider, SpecialCase, User, Users}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.AgentsExternalStubsStubs
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class AgentsExternalStubsConnectorISpec extends BaseISpec with AgentsExternalStubsStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector: AgentsExternalStubsConnector = app.injector.instanceOf[AgentsExternalStubsConnector]

  "AgentsExternalStubsConnector" when {

    "signIn" should {

      "return authenticated session with new user flag on" in {
        val sessionId = givenUserCanSignIn("foo", "bar")
        val response: AuthenticatedSession = await(connector.signIn(SignInRequest("foo", "bar")))
        response.authToken shouldBe sessionId
        response.userId shouldBe "foo"
        response.providerType shouldBe AuthProvider.GovernmentGateway
        response.newUserCreated shouldBe Some(true)
      }

      "return authenticated session with new user flag off" in {
        val sessionId = givenUserCanSignIn("foo", "bar", newUser = false)
        val response: AuthenticatedSession = await(connector.signIn(SignInRequest("foo", "bar")))
        response.authToken shouldBe sessionId
        response.userId shouldBe "foo"
        response.providerType shouldBe AuthProvider.GovernmentGateway
        response.newUserCreated shouldBe Some(false)
      }

      "throw a BadRequestException if the response is 400" in {
        stubFor(
          post(urlEqualTo(s"/agents-external-stubs/sign-in"))
            .willReturn(
              aResponse()
                .withStatus(Status.BAD_REQUEST)
                .withHeaders(new HttpHeaders(new HttpHeader(HeaderNames.LOCATION, "")))
            )
        )

        intercept[BadRequestException] {
          await(connector.signIn(SignInRequest("foo", "bar")))
        }
      }

      "throw an IllegalStateException if there is no Location header in the response" in {
        stubFor(
          post(urlEqualTo(s"/agents-external-stubs/sign-in"))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
            )
        )

        intercept[IllegalStateException] {
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
        val users: Users = await(connector.getUsers())
        users.users.map(_.userId) should contain.only("foo", "bar")
      }

      "omit empty query parameters" in {
        val connector = app.injector.instanceOf[AgentsExternalStubsConnector]

        connector.getUsers(
          userId = None,
          groupId = None,
          limit = None
        )

        verify(
          getRequestedFor(
            urlEqualTo("/agents-external-stubs/users")
          )
        )
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

    "getAllSpecialCases" should {
      "get a list of all special cases if 200" in {
        givenAllSpecialCases
        val result = await(connector.getAllSpecialCases)
        result should not be empty
        result should have size 3
      }

      "get an empty list" in {
        stubFor(
          get(urlEqualTo(s"/agents-external-stubs/special-cases"))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody("[]")
            )
        )
        val result = await(connector.getAllSpecialCases)
        result shouldBe empty
      }
    }

    "getSpecialCase" should {
      "return entity if success" in {
        stubFor(
          get(urlEqualTo(s"/agents-external-stubs/special-cases/123"))
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
                .withBody(validSpecialCaseResponse)
            )
        )
        val result: Option[SpecialCase] = await(connector.getSpecialCase("123"))
        result shouldBe defined
        result.map(_.requestMatch.path) shouldBe Some("/test")
        result.map(_.requestMatch.method) shouldBe Some("PUT")
        result.map(_.response.status) shouldBe Some(400)
        result.flatMap(_.response.body) shouldBe Some("{\"code\":\"MY_CODE\"}")
      }
    }

    "createSpecialCase" should {
      "return entity ID if success" in {
        stubFor(
          post(urlEqualTo(s"/agents-external-stubs/special-cases"))
            .withRequestBody(
              equalToJson(
                s"""{
                   |"requestMatch": {
                   |   "method": "PUT",
                   |   "path":"/test123"
                   | },
                   |"response": {
                   |   "status":474
                   | }
                   |}""".stripMargin,
                true,
                true
              )
            )
            .willReturn(
              aResponse()
                .withStatus(Status.CREATED)
                .withHeader(HeaderNames.LOCATION, "/agents-external-stubs/special-cases/123")
            )
        )
        val result: String =
          await(connector.createSpecialCase(SpecialCase(RequestMatch("/test123", "PUT"), SpecialCase.Response(474))))
        result shouldBe "123"
      }
    }

    "updateSpecialCase" should {
      "return entity ID if success" in {
        stubFor(
          put(urlEqualTo(s"/agents-external-stubs/special-cases/123"))
            .withRequestBody(
              equalToJson(
                s"""{
                   |  "requestMatch": {
                   |    "path":"/test321",
                   |    "method": "DELETE"
                   |  },
                   |  "response": {
                   |    "status":521
                   |  },
                   |  "id":"123"
                   |}""".stripMargin,
                true,
                true
              )
            )
            .willReturn(
              aResponse()
                .withStatus(Status.ACCEPTED)
                .withHeader(HeaderNames.LOCATION, "/agents-external-stubs/special-cases/123")
            )
        )
        val result: String =
          await(
            connector.updateSpecialCase(
              SpecialCase(RequestMatch("/test321", "DELETE"), SpecialCase.Response(521), id = Some("123"))
            )
          )
        result shouldBe "123"
      }
    }

    "deleteSpecialCase" should {
      "return unit if success" in {
        stubFor(
          delete(urlEqualTo(s"/agents-external-stubs/special-cases/123"))
            .willReturn(
              aResponse()
                .withStatus(Status.NO_CONTENT)
            )
        )
        val result: Unit = await(connector.deleteSpecialCase("123"))
        result shouldBe (())
      }
    }
  }

}
