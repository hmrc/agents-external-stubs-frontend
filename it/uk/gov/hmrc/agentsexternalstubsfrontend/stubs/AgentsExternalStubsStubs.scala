package uk.gov.hmrc.agentsexternalstubsfrontend.stubs

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.agentsexternalstubsfrontend.support.WireMockSupport

trait AgentsExternalStubsStubs {
  me: WireMockSupport =>

  def givenUserCanSignIn(
    userId: String,
    planetId: String,
    plainTextPassword: String = "p@ssw0rd",
    providerType: String = "GovernmentGateway",
    newUser: Boolean = true): String = {
    val authToken = UUID.randomUUID().toString

    stubFor(post(urlEqualTo(s"/agents-external-stubs/sign-in"))
      .withRequestBody(equalToJson(
        s"""{"userId":"$userId", "plainTextPassword":"$plainTextPassword", "providerType":"$providerType", "planetId": "$planetId"}"""))
      .willReturn(aResponse()
        .withStatus(if (newUser) Status.CREATED else Status.ACCEPTED)
        .withHeader("Location", s"/agents-external-stubs/authenticated/$authToken")))
    stubFor(
      get(urlEqualTo(s"/agents-external-stubs/authenticated/$authToken"))
        .willReturn(aResponse()
          .withStatus(Status.CREATED)
          .withBody(Json.obj("userId" -> userId, "authToken" -> authToken, "providerType" -> providerType).toString())))

    authToken
  }

  def givenUserCanSignOut =
    stubFor(
      get(urlEqualTo(s"/agents-external-stubs/sign-out"))
        .willReturn(aResponse()
          .withStatus(Status.NO_CONTENT)))

  def givenUser(user: User): Unit = {
    stubFor(
      get(urlEqualTo(s"/agents-external-stubs/users/${user.userId}"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(Json.toJson(user).toString())))
    stubFor(
      put(urlEqualTo(s"/agents-external-stubs/users/${user.userId}"))
        .willReturn(
          aResponse()
            .withStatus(Status.ACCEPTED)
            .withHeader("Location", s"/agents-external-stubs/users/${user.userId}")))
  }

}
