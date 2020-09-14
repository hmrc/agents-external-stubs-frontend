/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.IdentityVerificationController.UpliftRequest
import uk.gov.hmrc.play.test.UnitSpec

class UpliftFormSpec extends UnitSpec {

  "UpliftForm" should {

    "bind all input fields and return UpliftRequest and fill it back" in {
      val form = IdentityVerificationController.UpliftRequestForm

      val value = UpliftRequest("AB626225C", "success")

      val fieldValues = Map(
        "nino"   -> "AB626225C",
        "option" -> "success"
      )

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }
  }
}
