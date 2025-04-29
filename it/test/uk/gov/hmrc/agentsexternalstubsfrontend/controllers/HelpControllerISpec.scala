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

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec

class HelpControllerISpec extends BaseISpec with AgentsExternalStubsStubs with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "HelpController" when {

    "GET /agents-external-stubs/help/agent-authorisation-api" should {
      "render help page" in {
        val request = FakeRequest(GET, "/agents-external-stubs/help/agent-authorisation-api")
        val result = callEndpointWith(request)
        status(result) shouldBe 200
      }
    }

    "GET /agents-external-stubs/help/foobar" should {
      "return 400 Bad Request" in {
        val request = FakeRequest(GET, "/agents-external-stubs/help/foobar")
        val result = callEndpointWith(request)
        status(result) shouldBe 400
      }
    }

  }

}
