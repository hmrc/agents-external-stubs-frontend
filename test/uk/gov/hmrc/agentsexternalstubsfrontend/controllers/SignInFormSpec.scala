package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.play.test.UnitSpec

class SignInFormSpec extends UnitSpec {

  "SignInRequestForm" should {

    "bind some input fields and return SignInRequest and fill it back" in {
      val form = SignInController.SignInRequestForm

      val value = SignInRequest(userId = "bar", plainTextPassword = "foo", planetId = "juniper")

      val fieldValues =
        Map("userId" -> "bar", "password" -> "foo", "providerType" -> "GovernmentGateway", "planetId" -> "juniper")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }

    "bind all input fields and return SignInRequest and fill it back" in {
      val form = SignInController.SignInRequestForm

      val value = SignInRequest(userId = "foo", plainTextPassword = "bar", planetId = "juniper")

      val fieldValues =
        Map("userId" -> "foo", "password" -> "bar", "providerType" -> "GovernmentGateway", "planetId" -> "juniper")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }
  }
}
