package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.Credentials
import uk.gov.hmrc.play.test.UnitSpec

class LoginFormSpec extends UnitSpec {

  "LoginForm" should {

    "bind some input fields and return Credentials and fill it back" in {
      val form = SignInController.LoginForm

      val value = Credentials(userId = "bar", plainTextPassword = "foo")

      val fieldValues = Map("userId" -> "bar", "password" -> "foo")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }

    "bind all input fields and return Credentials and fill it back" in {
      val form = SignInController.LoginForm

      val value = Credentials(userId = "foo", plainTextPassword = "bar")

      val fieldValues = Map("userId" -> "foo", "password" -> "bar")

      form.bind(fieldValues).value shouldBe Some(value)
      form.fill(value).data shouldBe fieldValues
    }
  }
}
