/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.play.test.UnitSpec

class SignInFormSpec extends UnitSpec {

  "SignInRequestForm" should {

    "bind some input fields and return SignInRequest and fill it back" in {
      val form = SignInController.SignInRequestForm

      val value = SignInRequest(userId = "bar", plainTextPassword = "foo", planetId = "juniper")

      val fieldValues =
        Map(
          "userId"       -> "bar",
          "password"     -> "foo",
          "providerType" -> AuthProvider.GovernmentGateway,
          "planetId"     -> "juniper")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }

    "bind all input fields and return SignInRequest and fill it back" in {
      val form = SignInController.SignInRequestForm

      val value = SignInRequest(userId = "foo", plainTextPassword = "bar", planetId = "juniper")

      val fieldValues =
        Map(
          "userId"       -> "foo",
          "password"     -> "bar",
          "providerType" -> AuthProvider.GovernmentGateway,
          "planetId"     -> "juniper")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }
  }
}
