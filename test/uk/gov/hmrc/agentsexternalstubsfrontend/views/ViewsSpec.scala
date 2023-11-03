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

package uk.gov.hmrc.agentsexternalstubsfrontend.views

import java.util.UUID
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.IdentityVerificationController.UpliftRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.{IdentityVerificationController, routes}
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.{SignInRequest, SignInRequestForm}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AuthProvider
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.agentsexternalstubsfrontend.support.UnitSpec

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

  private val filledForm = SignInRequestForm.form.fill(
    SignInRequest(userId = "My contact name", plainTextPassword = "AA1 1AA", planetId = "juniper")
  )

  implicit val lang: Lang = Lang("en")
  implicit val requestHeader: RequestHeader = FakeRequest()
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val signInForm: sign_in = app.injector.instanceOf[sign_in]
  val errorTemplateView: error_template = app.injector.instanceOf[error_template]
  val upLiftView: iv_uplift = app.injector.instanceOf[iv_uplift]
  val mainTemplateView: main_template = app.injector.instanceOf[main_template]
  val appMessages: Messages = messagesApi.preferred(Seq())

  "signIn view" should {
    "render title and messages" in new App {
      val html = signInForm
        .render(
          loginForm = filledForm,
          postUrl = routes.SignInController.signIn(None, Some("foo"), None, AuthProvider.GovernmentGateway),
          request = FakeRequest(),
          msgs = appMessages
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
          appMessages
        )
      contentAsString(html2) shouldBe content
    }
  }

  "iv_uplift view" should {
    val filledUpliftForm = IdentityVerificationController.UpliftRequestForm.fill(
      UpliftRequest("AB 62 62 25 C", "success")
    )

    "render title and messages" in new App {
      val postCall = routes.IdentityVerificationController
        .upliftProxy("123456789", 200, RedirectUrl("/good"), RedirectUrl("/bad"), Some("aif"))
      val html = upLiftView
        .render(
          filledUpliftForm,
          options(journeyIdValue),
          postCall,
          FakeRequest(),
          appMessages,
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
            appMessages,
            app.configuration
          )
      contentAsString(html2) shouldBe content
    }
  }

  "error_template view" should {
    "render title, heading and message" in new App {
      val pageTitle = "my.custom.page.title"
      val heading = "my.custom.heading"
      val message = "my.custom.message"
      val html = errorTemplateView.render(
        pageTitleMsgKey = pageTitle,
        headingMsgKey = heading,
        messageMsgKey = message,
        appMessages,
        FakeRequest()
      )

      val content = contentAsString(html)
      content should include(pageTitle)
      content should include(heading)
      content should include(message)

      val html2 =
        errorTemplateView.f(pageTitle, heading, message)(appMessages, FakeRequest())
      contentAsString(html2) shouldBe content
    }
  }

  "main_template view" should {
    "render all supplied arguments" in new App {
      val view = mainTemplateView
      val html = view.render(
        title = "My custom page title",
        backLinkHref = Some("My custom backlink"),
        showSignOut = true,
        wide = false,
        sidebar = Some(Html("My custom sidebar links")),
        mainContent = Html("My custom main content HTML"),
        request = FakeRequest(),
        msgs = appMessages
      )

      val content = contentAsString(html)
      content should include("My custom page title")
      content should include("My custom sidebar links")
      content should include("My custom backlink")
      content should include("My custom main content HTML")

      val html2 = view.f(
        "My custom page title",
        Some("My custom backlink"),
        true,
        false,
        Some(Html("My custom sidebar links"))
      )(Html("My custom main content HTML"))(FakeRequest(), appMessages)
      contentAsString(html2) shouldBe content
    }
  }
}
