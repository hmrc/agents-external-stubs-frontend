package uk.gov.hmrc.agentsexternalstubsfrontend.stubs

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubsfrontend.support.WireMockSupport

trait AgentsExternalStubsStubs {
  me: WireMockSupport =>

  def givenUserCanSignIn(userId: String, password: String, providerType: String = "GovernmentGateway"): String = {
    val authToken = UUID.randomUUID().toString

    stubFor(
      post(urlEqualTo(s"/agents-external-stubs/sign-in"))
        .withRequestBody(
          equalToJson(s"""{"userId":"$userId", "plainTextPassword":"$password", "providerType":"$providerType"}"""))
        .willReturn(aResponse()
          .withStatus(Status.CREATED)
          .withHeader("Location", s"/agents-external-stubs/authenticated/$authToken")))
    stubFor(
      get(urlEqualTo(s"/agents-external-stubs/authenticated/$authToken"))
        .willReturn(aResponse()
          .withStatus(Status.CREATED)
          .withBody(Json.obj("userId" -> userId, "authToken" -> authToken, "providerType" -> providerType).toString())))

    authToken
  }

}
