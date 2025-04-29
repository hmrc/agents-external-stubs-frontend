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

package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import scala.concurrent.Future

class AuthActionsISpec extends BaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Bearer XYZ")))
    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorisedAsAgent[A]: Result =
      await(super.withAuthorisedAsAgent(arn => Future.successful(Ok(arn.value))))

    def withAuthorisedAsClient[A]: Result =
      await(super.withAuthorisedAsClient(mtdItTd => Future.successful(Ok(mtdItTd.value))))

  }

  "withAuthorisedAsAgent" should {

    "call body with arn when valid agent" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin
      )
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooArn"
    }

    "throw AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }

    "throw InsufficientEnrolments when agent not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }

    "throw InsufficientEnrolments when expected agent's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"BAR", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }
  }

  "withAuthorisedAsClient" should {

    "call body with mtditid when valid mtd client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin
      )

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooMtdItId"
    }

    "throw InsufficientEnrolments when client not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsClient
      }
    }

    "throw InsufficientEnrolments when expected client's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"BAR", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsClient
      }
    }
  }

}
