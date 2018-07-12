package uk.gov.hmrc.agentsexternalstubsfrontend.views

import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.{LoginController, routes}
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.LoginController.Credentials
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.error_template_Scope0.error_template_Scope1.error_template
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.govuk_wrapper_Scope0.govuk_wrapper_Scope1.govuk_wrapper
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.login_form_Scope0.login_form_Scope1.login_form
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.main_template_Scope0.main_template_Scope1.main_template
import uk.gov.hmrc.play.test.UnitSpec

class ViewsSpec extends UnitSpec with OneAppPerSuite {

  private val filledForm = LoginController.LoginForm.fill(
    Credentials(userId = "My contact name", plainTextPassword = "AA1 1AA")
  )

  "login view" should {
    "render title and messages" in new App {
      val html = new login_form()
        .render(
          loginForm = filledForm,
          postUrl = routes.LoginController.login(None, "", None),
          request = FakeRequest(),
          messages = Messages.Implicits.applicationMessages,
          config = app.configuration
        )
      val content = contentAsString(html)

      import Messages.Implicits.applicationMessages
      content should include(Messages("start.title"))
      content should include(Messages("start.label"))
      content should include(Messages("start.intro"))
      content should include(Messages("start.helpdesklink.text1"))
      content should include(Messages("start.helpdesklink.text2"))
      content should include(Messages("login.sa.header"))
      content should include(Messages("login.sa.description"))
      content should include(Messages("login.username"))
      content should include(Messages("login.password"))

      val html2 = new login_form().f(filledForm, routes.LoginController.login(None, "", None))(
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
      val html = new error_template().render(
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
        new error_template().f(pageTitle, heading, message)(Messages.Implicits.applicationMessages, app.configuration)
      contentAsString(html2) shouldBe (content)
    }
  }

  "main_template view" should {
    "render all supplied arguments" in new App {
      val view = new main_template()
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

      val html = new govuk_wrapper().render(
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

      val html2 = new govuk_wrapper().f(
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
