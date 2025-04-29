/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend.support

import org.apache.pekko.stream.Materializer
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AuthStubs, DataStreamStubs}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.Future

class BaseISpec
    extends AnyWordSpecLike with GuiceOneAppPerSuite with WireMockSupport with AuthStubs with DataStreamStubs
    with DefaultAwaitTimeout with Matchers with OptionValues with ScalaFutures {

  override implicit lazy val app: Application = appBuilder.build()

  val appConfig: FrontendConfig = app.injector.instanceOf[FrontendConfig]

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.agents-external-stubs.port" -> wireMockPort,
        "microservice.services.auth.port"                  -> wireMockPort
      )

  override def commonStubs(): Unit = {
    givenAuditConnector()
  }

  protected implicit val materializer: Materializer = app.materializer

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Unit = {
    status(result) shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  // the following is a collection of useful methods that should minimise
  // the changes required when migrating away from hmrctest, which is now deprecated.
  def status(result: Result): Int = result.header.status
  def status(result: Future[Result]): Int = Helpers.status(result)
  def bodyOf(result: Result): String = Helpers.contentAsString(Future.successful(result))
  def redirectLocation(result: Result): Option[String] = Helpers.redirectLocation(Future.successful(result))

  def contentType(result: Result): Option[String] =
    result.body.contentType.map(_.split(";").take(1).mkString.trim)

  def charset(result: Result): Option[String] =
    result.body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _                                 => None
    }

}
