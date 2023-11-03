/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend.forms

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{EnrolmentKey, Identifier, User}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.UnitSpec
import uk.gov.hmrc.domain.Nino

class UserFormSpec extends UnitSpec {

  "UserForm" should {

    "bind some input fields and return Individual user and fill it back" in {
      val form = UserForm.form

      val value = User(
        userId = "bar",
        confidenceLevel = Some(50),
        nino = Some(Nino("HW827856C"))
      )

      val fieldValues = Map(
        "userId"             -> "bar",
        "confidenceLevel"    -> "50",
        "credentialStrength" -> "none",
        "strideRoles"        -> "",
        "credentialRole"     -> "none",
        "nino"               -> "HW827856C"
      )

      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
      form.fill(value).data shouldBe fieldValues.-("userId")
    }

    "bind some input fields and return user and fill it back" in {
      val form = UserForm.form

      val value = User(userId = "bar")

      val fieldValues =
        Map(
          "userId"             -> "bar",
          "confidenceLevel"    -> "0",
          "credentialStrength" -> "none",
          "strideRoles"        -> "",
          "credentialRole"     -> "none"
        )

      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
      form.fill(value).data shouldBe fieldValues.-("userId")
    }

    "bind all input fields and return User and fill it back" in {
      val form = UserForm.form

      val value =
        User(
          userId = "foo",
          groupId = Some("ABA-712-878-NHG"),
          confidenceLevel = None,
          nino = None,
          credentialStrength = Some("strong"),
          credentialRole = Some("Admin"),
          assignedPrincipalEnrolments =
            Seq(EnrolmentKey("FOO", Seq.empty), EnrolmentKey("BAR", Seq(Identifier("ABC", "123")))),
          assignedDelegatedEnrolments =
            Seq(EnrolmentKey("TAR", Seq(Identifier("XYZ", "987"))), EnrolmentKey("ZOO", Seq.empty))
        )

      val fieldValues = Map(
        "credentialStrength"                                  -> "strong",
        "strideRoles"                                         -> "",
        "assignedDelegatedEnrolments[0].key"                  -> "TAR",
        "assignedDelegatedEnrolments[0].identifiers[0].key"   -> "XYZ",
        "assignedDelegatedEnrolments[0].identifiers[0].value" -> "987",
        "assignedDelegatedEnrolments[1].key"                  -> "ZOO",
        "assignedPrincipalEnrolments[0].key"                  -> "FOO",
        "assignedPrincipalEnrolments[1].key"                  -> "BAR",
        "assignedPrincipalEnrolments[1].identifiers[0].key"   -> "ABC",
        "assignedPrincipalEnrolments[1].identifiers[0].value" -> "123",
        "confidenceLevel"                                     -> "0",
        "userId"                                              -> "foo",
        "groupId"                                             -> "ABA-712-878-NHG",
        "credentialRole"                                      -> "Admin"
      )

      form.fill(value).data shouldBe fieldValues.-("userId")
      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
    }

    "bind all input fields and return User and fill it back when enrolments empty" in {
      val form = UserForm.form

      val value =
        User(
          userId = "foo",
          groupId = Some("ABA-712-878-NHG"),
          confidenceLevel = None,
          nino = None,
          strideRoles = Seq("A", "BB", "cac"),
          credentialStrength = Some("strong"),
          credentialRole = Some("Admin")
        )

      val fieldValues = Map(
        "credentialStrength" -> "strong",
        "strideRoles"        -> "A,BB,cac",
        "confidenceLevel"    -> "0",
        "userId"             -> "foo",
        "groupId"            -> "ABA-712-878-NHG",
        "credentialRole"     -> "Admin"
      )

      form.fill(value).data shouldBe fieldValues.-("userId")
      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
    }
  }
}
