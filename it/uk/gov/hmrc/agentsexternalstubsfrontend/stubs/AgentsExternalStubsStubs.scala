package uk.gov.hmrc.agentsexternalstubsfrontend.stubs

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubsfrontend.support.WireMockSupport

trait AgentsExternalStubsStubs {
  me: WireMockSupport =>

  def givenUserCanSignIn(username: String, password: String): String = {
    val sessionId = UUID.randomUUID().toString

    stubFor(
      post(urlEqualTo(s"/agents-external-stubs/sign-in"))
        .withRequestBody(equalToJson(s"""{"userId":"$username","plainTextPassword":"$password"}"""))
        .willReturn(aResponse()
          .withStatus(Status.CREATED)
          .withHeader("Location", s"/agents-external-stubs/session/$sessionId")))
    stubFor(
      get(urlEqualTo(s"/agents-external-stubs/session/$sessionId"))
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
            .withBody(Json.obj("sessionId" -> sessionId).toString())))

    sessionId
  }

}
