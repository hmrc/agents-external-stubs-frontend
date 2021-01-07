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

package uk.gov.hmrc.agentsexternalstubsfrontend.views

import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesProvider}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.IdentityVerificationController.UpliftRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.{IdentityVerificationController, SignInController, routes}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html._
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.test.UnitSpec

class ViewsSpec extends UnitSpec with GuiceOneAppPerSuite {

  val journeyIdValue: String = UUID.randomUUID().toString

  val options: String => Seq[(String, String)] = (journeyId: String) =>
    Seq(
      s"success~$journeyId"              -> "Success",
      s"preconditionFailed~$journeyId"   -> "Precondition Failed",
      s"lockedOut~$journeyId"            -> "Locked Out",
      s"insufficientEvidence~$journeyId" -> "Insufficient Evidence",
      s"failedMatching~$journeyId"       -> "Failed Matching",
      s"technicalIssue~$journeyId"       -> "Technical Issue",
      s"userAborted~$journeyId"          -> "User Aborted",
      s"timedOut~$journeyId"             -> "Timed Out",
      s"failedIV~$journeyId"             -> "Failed IV"
  )

  private val filledForm = SignInController.SignInRequestForm.fill(
    SignInRequest(userId = "My contact name", plainTextPassword = "AA1 1AA", planetId = "juniper")
  )

  implicit val lang: Lang = Lang("en")
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val signInForm = app.injector.instanceOf[sign_in]
  val errorTemplateView = app.injector.instanceOf[error_template]
  val upLiftView = app.injector.instanceOf[iv_uplift]
  val govUkWrapper = app.injector.instanceOf[govuk_wrapper]
  val mainTemplateView = app.injector.instanceOf[main_template]

  "signIn view" should {
    "render title and messages" in new App {
      val html = signInForm
        .render(
          loginForm = filledForm,
          postUrl = routes.SignInController.signIn(None, Some("foo"), None, AuthProvider.GovernmentGateway),
          request = FakeRequest(),
          messages = Messages.Implicits.applicationMessages,
          config = app.configuration
        )
      val content = contentAsString(html)
      content should include(messagesApi("start.title"))
      content should include(messagesApi("start.label"))
      content should include(messagesApi("start.intro"))
      content should include(messagesApi("start.helpdesklink.text1"))
      content should include(messagesApi("start.helpdesklink.text2"))
      content should include(messagesApi("login.sa.header"))
      content should include(messagesApi("login.sa.description"))
      content should include(messagesApi("login.username"))
      content should include(messagesApi("login.password"))

      val html2 =
        signInForm.f(filledForm, routes.SignInController.signIn(None, None, None, AuthProvider.GovernmentGateway))(
          FakeRequest(),
          Messages.Implicits.applicationMessages,
          app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }

  "iv_uplift view" should {
    val filledUpliftForm = IdentityVerificationController.UpliftRequestForm.fill(
      UpliftRequest("AB 62 62 25 C", "success")
    )

    "render title and messages" in new App {
      val postCall = routes.IdentityVerificationController
        .upliftProxy("123456789", 200, ContinueUrl("/good"), ContinueUrl("/bad"), Some("aif"))
      val html = upLiftView
        .render(
          filledUpliftForm,
          options(journeyIdValue),
          postCall,
          FakeRequest(),
          Messages.Implicits.applicationMessages,
          app.configuration
        )
      val content = contentAsString(html)

      content should include(messagesApi("uplift.header"))
      content should include(messagesApi("uplift.description"))
      content should include(messagesApi("uplift.form.dropdown.confidenceLevel"))
      content should include(messagesApi("uplift.form.radio.willSucceed"))
      content should include(messagesApi("uplift.form.radio.willFail"))
      content should include(messagesApi("uplift.form.submit"))

      val html2 =
        upLiftView
          .f(filledUpliftForm, options(journeyIdValue), postCall)(
            FakeRequest(),
            Messages.Implicits.applicationMessages,
            app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }

  "error_template view" should {
    "render title, heading and message" in new App {
      val pageTitle = "My custom page title"
      val heading = "My custom heading"
      val message = "My custom message"
      val html = errorTemplateView.render(
        pageTitle = pageTitle,
        heading = heading,
        message = message,
        messages = Messages.Implicits.applicationMessages,
        configuration = app.configuration)
      val content = contentAsString(html)
      content should include(pageTitle)
      content should include(heading)
      content should include(message)

      val html2 =
        errorTemplateView.f(pageTitle, heading, message)(Messages.Implicits.applicationMessages, app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }

  "main_template view" should {
    "render all supplied arguments" in new App {
      val view = mainTemplateView
      val html = view.render(
        title = "My custom page title",
        sidebarLinks = Some(Html("My custom sidebar links")),
        contentHeader = Some(Html("My custom content header")),
        bodyClasses = Some("my-custom-body-class"),
        mainClass = Some("my-custom-main-class"),
        scriptElem = Some(Html("My custom script")),
        mainContent = Html("My custom main content HTML"),
        messages = Messages.Implicits.applicationMessages,
        request = FakeRequest(),
        configuration = app.configuration
      )

      val content = contentAsString(html)
      content should include("My custom page title")
      content should include("My custom sidebar links")
      content should include("My custom content header")
      content should include("my-custom-body-class")
      content should include("my-custom-main-class")
      content should include("My custom script")
      content should include("My custom main content HTML")

      val html2 = view.f(
        "My custom page title",
        Some(Html("My custom sidebar links")),
        Some(Html("My custom content header")),
        Some("my-custom-body-class"),
        Some("my-custom-main-class"),
        Some(Html("My custom script"))
      )(Html("My custom main content HTML"))(Messages.Implicits.applicationMessages, FakeRequest(), app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }

  "govuk wrapper view" should {
    "render all of the supplied arguments" in new App {

      val html = govUkWrapper.render(
        title = "My custom page title",
        mainClass = Some("my-custom-main-class"),
        mainDataAttributes = Some(Html("myCustom=\"attributes\"")),
        bodyClasses = Some("my-custom-body-class"),
        sidebar = Html("My custom sidebar"),
        contentHeader = Some(Html("My custom content header")),
        mainContent = Html("My custom main content"),
        serviceInfoContent = Html("My custom service info content"),
        scriptElem = Some(Html("My custom script")),
        gaCode = Seq("My custom GA code"),
        messages = Messages.Implicits.applicationMessages,
        configuration = app.configuration
      )

      val content = contentAsString(html)
      content should include("My custom page title")
      content should include("my-custom-main-class")
      content should include("myCustom=\"attributes\"")
      content should include("my-custom-body-class")
      content should include("My custom sidebar")
      content should include("My custom content header")
      content should include("My custom main content")
      content should include("My custom service info content")
      content should include("My custom script")

      val html2 = govUkWrapper.f(
        "My custom page title",
        Some("my-custom-main-class"),
        Some(Html("myCustom=\"attributes\"")),
        Some("my-custom-body-class"),
        Html("My custom sidebar"),
        Some(Html("My custom content header")),
        Html("My custom main content"),
        Html("My custom service info content"),
        Some(Html("My custom script")),
        Seq("My custom GA code")
      )(Messages.Implicits.applicationMessages, app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }
}
