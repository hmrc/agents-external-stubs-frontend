/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, EnrolmentKey, Enrolments, Identifier, User}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentsexternalstubsfrontend.support.UnitSpec

class UserFormSpec extends UnitSpec {

  "UserForm" should {

    "bind some input fields and return Individual user and fill it back" in {
      val form = UserController.UserForm

      val value = User(
        userId = "bar",
        affinityGroup = Some(User.Individual),
        confidenceLevel = Some(50),
        nino = Some(Nino("HW827856C"))
      )

      val fieldValues = Map(
        "userId"             -> "bar",
        "confidenceLevel"    -> "50",
        "affinityGroup"      -> "Individual",
        "credentialStrength" -> "none",
        "strideRoles"        -> "",
        "credentialRole"     -> "none",
        "nino"               -> "HW827856C"
      )

      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
      form.fill(value).data shouldBe fieldValues.-("userId")
    }

    "bind some input fields and return user and fill it back" in {
      val form = UserController.UserForm

      val value = User(userId = "bar")

      val fieldValues =
        Map(
          "userId"             -> "bar",
          "affinityGroup"      -> "none",
          "confidenceLevel"    -> "0",
          "credentialStrength" -> "none",
          "strideRoles"        -> "",
          "credentialRole"     -> "none"
        )

      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
      form.fill(value).data shouldBe fieldValues.-("userId")
    }

    "bind all input fields and return User and fill it back" in {
      val form = UserController.UserForm

      val value =
        User(
          userId = "foo",
          groupId = Some("ABA-712-878-NHG"),
          confidenceLevel = None,
          nino = None,
          credentialStrength = Some("strong"),
          affinityGroup = Some("Agent"),
          credentialRole = Some("Admin"),
          enrolments = Some(
            Enrolments(
              principal = Seq(Enrolment("FOO"), Enrolment("BAR", Some(Seq(Identifier("ABC", "123"))))),
              delegated = Seq(Enrolment("TAR", Some(Seq(Identifier("XYZ", "987")))), Enrolment("ZOO")),
              assigned = Seq(EnrolmentKey("FOO", Seq(Identifier("BAR", "BAZ"))))
            )
          ),
          suspendedRegimes = Some(Set("ITSA"))
        )

      val fieldValues = Map(
        "affinityGroup"                                -> "Agent",
        "credentialStrength"                           -> "strong",
        "strideRoles"                                  -> "",
        "enrolments.delegated[0].key"                  -> "TAR",
        "enrolments.delegated[0].identifiers[0].key"   -> "XYZ",
        "enrolments.delegated[0].identifiers[0].value" -> "987",
        "enrolments.delegated[1].key"                  -> "ZOO",
        "enrolments.principal[0].key"                  -> "FOO",
        "enrolments.principal[1].key"                  -> "BAR",
        "enrolments.principal[1].identifiers[0].key"   -> "ABC",
        "enrolments.principal[1].identifiers[0].value" -> "123",
        "enrolments.assigned[0].key"                   -> "FOO",
        "enrolments.assigned[0].identifiers[0].key"    -> "BAR",
        "enrolments.assigned[0].identifiers[0].value"  -> "BAZ",
        "confidenceLevel"                              -> "0",
        "userId"                                       -> "foo",
        "groupId"                                      -> "ABA-712-878-NHG",
        "credentialRole"                               -> "Admin",
        "suspendedRegimes[0]"                          -> "ITSA"
      )

      form.fill(value).data shouldBe fieldValues.-("userId")
      form.bind(fieldValues).value shouldBe Some(value.copy(userId = "ignored"))
    }

    "bind all input fields and return User and fill it back when enrolments empty" in {
      val form = UserController.UserForm

      val value =
        User(
          userId = "foo",
          groupId = Some("ABA-712-878-NHG"),
          confidenceLevel = None,
          nino = None,
          strideRoles = Seq("A", "BB", "cac"),
          credentialStrength = Some("strong"),
          affinityGroup = Some("Agent"),
          credentialRole = Some("Admin")
        )

      val fieldValues = Map(
        "affinityGroup"      -> "Agent",
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
