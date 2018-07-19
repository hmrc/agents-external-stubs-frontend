package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserFormSpec extends UnitSpec {

  "UserForm" should {

    "bind some input fields and return User and fill it back" in {
      val form = UserController.UserForm

      val value = User(userId = "bar")

      val fieldValues = Map("userId" -> "bar", "confidenceLevel" -> "50")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }

    "bind all input fields and return User and fill it back" in {
      val form = UserController.UserForm

      val value =
        User(
          userId = "foo",
          groupId = Some("ABA-712-878-NHG"),
          confidenceLevel = 100,
          nino = Some(Nino("HW827856C")),
          credentialStrength = Some("strong"),
          affinityGroup = Some("Agent"),
          credentialRole = Some("Admin"),
          principalEnrolments = Seq(Enrolment("FOO"), Enrolment("BAR", Some(Seq(Identifier("ABC", "123"))))),
          delegatedEnrolments = Seq(Enrolment("TAR", Some(Seq(Identifier("XYZ", "987")))), Enrolment("ZOO"))
        )

      val fieldValues = Map(
        "principalEnrolments[1].identifiers[0].key"   -> "ABC",
        "nino"                                        -> "HW827856C",
        "affinityGroup"                               -> "Agent",
        "credentialStrength"                          -> "strong",
        "delegatedEnrolments[0].key"                  -> "TAR",
        "principalEnrolments[1].key"                  -> "BAR",
        "delegatedEnrolments[0].identifiers[0].key"   -> "XYZ",
        "principalEnrolments[1].identifiers[0].value" -> "123",
        "confidenceLevel"                             -> "100",
        "principalEnrolments[0].key"                  -> "FOO",
        "delegatedEnrolments[1].key"                  -> "ZOO",
        "delegatedEnrolments[0].identifiers[0].value" -> "987",
        "userId"                                      -> "foo",
        "groupId"                                     -> "ABA-712-878-NHG",
        "credentialRole"                              -> "Admin"
      )

      form.fill(value).data shouldBe fieldValues
      form.bind(fieldValues).value shouldBe Some(value)
    }
  }
}
