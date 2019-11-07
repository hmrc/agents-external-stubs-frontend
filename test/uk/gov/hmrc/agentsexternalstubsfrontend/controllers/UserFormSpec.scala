package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserFormSpec extends UnitSpec {

  "UserForm" should {

    "bind some input fields and return Individual user and fill it back" in {
      val form = UserController.UserForm

      val value = User(
        userId = "bar",
        affinityGroup = Some(User.Individual),
        confidenceLevel = Some(50),
        nino = Some(Nino("HW827856C")))

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
          "credentialRole"     -> "none")

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
          principalEnrolments = Some(Seq(Enrolment("FOO"), Enrolment("BAR", Some(Seq(Identifier("ABC", "123")))))),
          delegatedEnrolments = Some(Seq(Enrolment("TAR", Some(Seq(Identifier("XYZ", "987")))), Enrolment("ZOO"))),
          suspendedServices = Some(Seq("HMRC-MTD-IT"))
        )

      val fieldValues = Map(
        "principalEnrolments[1].identifiers[0].key"   -> "ABC",
        "affinityGroup"                               -> "Agent",
        "credentialStrength"                          -> "strong",
        "strideRoles"                                 -> "",
        "delegatedEnrolments[0].key"                  -> "TAR",
        "principalEnrolments[1].key"                  -> "BAR",
        "delegatedEnrolments[0].identifiers[0].key"   -> "XYZ",
        "principalEnrolments[1].identifiers[0].value" -> "123",
        "confidenceLevel"                             -> "0",
        "principalEnrolments[0].key"                  -> "FOO",
        "delegatedEnrolments[1].key"                  -> "ZOO",
        "delegatedEnrolments[0].identifiers[0].value" -> "987",
        "userId"                                      -> "foo",
        "groupId"                                     -> "ABA-712-878-NHG",
        "credentialRole"                              -> "Admin",
        "suspendedServices[0]"                        -> "HMRC-MTD-IT"
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
