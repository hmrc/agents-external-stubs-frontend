/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubsfrontend.stubs.{AgentsExternalStubsStubs, AuthStubs}
import uk.gov.hmrc.agentsexternalstubsfrontend.support.BaseISpec
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{EnrolmentKey, Identifier, User}

import scala.concurrent.Future

class UserControllerISpec
  extends BaseISpec
    with AgentsExternalStubsStubs
    with AuthStubs {

  def callEndpointWith[A: Writeable](request: Request[A]): Result =
    await(play.api.test.Helpers.route(app, request).get)

  "UserController" when {

    "GET /agents-external-stubs/user" should {
      "display current user details" in {
        givenAuthorised("Test123")
        givenUser(User("Test123"))

        val request =
          FakeRequest(GET, "/agents-external-stubs/user")
            .withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Test123"))
      }
    }

    "GET /agents-external-stubs/users" should {

      def enrolmentKeyForService(service: String): EnrolmentKey =
        EnrolmentKey(service, Seq(Identifier("key", "value")))
      val usersList: List[User] = List(
        User("foo", groupId = Some("group1"), assignedPrincipalEnrolments = Seq(enrolmentKeyForService("HMRC-MTD-IT"))),
        User("bar", groupId = Some("group2"), assignedPrincipalEnrolments = Seq(enrolmentKeyForService("HMRC-MTD-IT"))),
        User("fizz", groupId = Some("group1"), assignedPrincipalEnrolments = Seq(enrolmentKeyForService("something-else"))),
        User("buzz", groupId = Some("group1"), assignedPrincipalEnrolments = Seq(enrolmentKeyForService("HMRC-MTD-IT"))),
      )

      //        TODO: Need to correct/improve form of testing which results are returned for getUsers tests below!!! Use Jsoup

      "render users page with no filters" in {
        givenAuthorised()
        givenUsers(usersList: _*)

        val request =
          FakeRequest(GET, "/agents-external-stubs/users")
            .withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("foo", "bar", "fizz", "buzz").forall(body.contains)

        expectedUsersDisplayed shouldBe true
      }

      "allow partial filtering by userId parameter" in {
        val userId = "zz"

        givenAuthorised()
        givenUsersWithUserId(userId, usersList: _*)

        val request =
          FakeRequest(
            GET,
            s"/agents-external-stubs/users?userId=$userId"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("fizz", "buzz").forall(body.contains)
        val expectedUsersNotDisplayed = Seq("foo", "bar").forall(!body.contains(_))

        expectedUsersDisplayed shouldBe true
        expectedUsersNotDisplayed shouldBe true
      }

      "allow filtering by groupId parameter" in {
        val groupId = "group1"

        givenAuthorised()
        givenUsersWithGroupId(groupId, usersList: _*)

        val request =
          FakeRequest(
            GET,
            s"/agents-external-stubs/users?groupId=$groupId"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("foo", "fizz", "buzz").forall(body.contains)
        val expectedUsersNotDisplayed = Seq("bar").forall(!body.contains(_))

        expectedUsersDisplayed shouldBe true
        expectedUsersNotDisplayed shouldBe true
      }

      "allow filtering by principalEnrolmentService parameter" in {
        val principalEnrolmentService = "HMRC-MTD-IT"

        givenAuthorised()
        givenUsersWithPrincipalEnrolmentService(principalEnrolmentService, usersList: _*)

        val request =
          FakeRequest(
            GET,
            s"/agents-external-stubs/users?principalEnrolmentService=$principalEnrolmentService"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("foo", "bar", "buzz").forall(body.contains)
        val expectedUsersNotDisplayed = Seq("fizz").forall(!body.contains(_))

        expectedUsersDisplayed shouldBe true
        expectedUsersNotDisplayed shouldBe true
      }

      "allow limiting results by limit parameter" in {
        givenAuthorised()
        val limit = 2
        givenUsersWithLimit(limit, usersList: _*)

        val request =
          FakeRequest(
            GET,
            s"/agents-external-stubs/users?limit=$limit"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("foo", "bar").forall(body.contains)
        val expectedUsersNotDisplayed = Seq("fizz", "buzz").forall(!body.contains(_))

        expectedUsersDisplayed shouldBe true
        expectedUsersNotDisplayed shouldBe true
      }

      "allow filtering by userId, groupId, principalEnrolmentService and limit parameters" in {
        val userId = "zz"
        val groupId = "group1"
        val principalEnrolmentService = "HMRC-MTD-IT"
        val limit = 2

        givenAuthorised()
        givenUsersWithPrincipalEnrolmentService("HMRC-MTD-IT", usersList: _*)

        val stubsUrl = s"/agents-external-stubs/users?limit=$limit&userId=$userId&groupId=$groupId&principalEnrolmentService=$principalEnrolmentService"

        val request =
          FakeRequest(
            GET,
            stubsUrl
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(result, htmlEscapedMessage("Your test users"))

        val body = contentAsString(Future.successful(result))
        val expectedUsersDisplayed = Seq("buzz").forall(body.contains)
        val expectedUsersNotDisplayed = Seq("foo", "bar", "fizz").forall(!body.contains(_))

        expectedUsersDisplayed shouldBe true
        expectedUsersNotDisplayed shouldBe true
      }

      "return an error when non-numeric entry to limit" in {
        givenAuthorised("Test123")
        givenUsers(
          User("Test123")
        )

        val request =
          FakeRequest(
            GET,
            "/agents-external-stubs/users?limit=abc"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultWithBodyText(
          result,
          htmlEscapedMessage("Numeric value expected")
        )
      }

      "preserve filter values after submitting filters" in {
//        TODO: Add additional principalEnrolmentService filter field to this test
        givenAuthorised("Test123")
        givenUsers(User("Test123"), User("test456"))

        val request =
          FakeRequest(
            GET,
            "/agents-external-stubs/users?userId=Test&groupId=G1&limit=10"
          ).withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200

        val body = contentAsString(Future.successful(result))

        body should include("""name="userId"""")
        body should include("""value="Test"""")
        body should include("""name="groupId"""")
        body should include("""value="G1"""")
        body should include("""name="limit"""")
        body should include("""value="10"""")
      }

      "render clear filters button" in {
        givenAuthorised("Test123")
        givenUsers(User("Test123"))

        val request =
          FakeRequest(GET, "/agents-external-stubs/users?userId=Test&limit=10")
            .withSession("authToken" -> "Bearer XYZ")

        val result = callEndpointWith(request)

        status(result) shouldBe 200

        val body = contentAsString(Future.successful(result))

        body should include("Clear filters")
        body should include("""href="/agents-external-stubs/users"""")
      }
    }
  }
}
