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

package uk.gov.hmrc.agentsexternalstubsfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) =
    authenticated(request, Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn), isAgent = true)

  def authenticated[A](request: FakeRequest[A], enrolment: Enrolment, isAgent: Boolean): FakeRequest[A] = {
    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
         |    { "authProviders": [AuthProvider.GovernmentGateway] }
         |  ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"authorisedEnrolments": [
         |  { "key":"${enrolment.serviceName}", "identifiers": [
         |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
         |  ]}
         |]}
          """.stripMargin
    )
    request.withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")
        )
    )

  def givenAuthorised(userId: String = "foo"): Unit = {
    givenAuthorisedFor(
      """{"retrieve": []}""",
      s"""{}""".stripMargin
    )
    givenAuthorisedFor(
      """{"retrieve": ["credentials"]}""",
      s"""{
         |"credentials":{
         |    "providerId": "$userId",
         |    "providerType": "bar",
         |    "planetId": "foobar"
         |  }
         |}""".stripMargin
    )
  }

  def givenAuthorisedFor(payload: String, responseBody: String): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        )
    )
  }

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
