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

package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, stubFor, urlEqualTo}
import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class PlanetControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "PlanetController" when {

    "GET /agents-external-stubs/planet/destroy" should {
      "destroy existing test planet and redirect to the start page" in {
        givenAuthorised("Test123")
        stubFor(
          delete(urlEqualTo("/agents-external-stubs/planets/foobar"))
            .willReturn(
              aResponse()
                .withStatus(204)
            )
        )

        val request = FakeRequest(GET, "/agents-external-stubs/planet/destroy").withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/agents-external-stubs")
      }
    }

  }

}
