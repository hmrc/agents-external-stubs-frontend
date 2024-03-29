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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import play.api.http.Status

import java.net.URL
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedSession(
  sessionId: String,
  userId: String,
  authToken: String,
  providerType: String,
  planetId: String,
  newUserCreated: Option[Boolean] = None
)

object AuthenticatedSession {
  implicit val reads: Reads[AuthenticatedSession] = Json.reads[AuthenticatedSession]
}

@Singleton
class AgentsExternalStubsConnector @Inject() (appConfig: FrontendConfig, http: HttpClient, httpV2: HttpClientV2) {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  val baseUrl: String = appConfig.aesBaseUrl

  def signIn(
    credentials: SignInRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] =
    http
      .POST[SignInRequest, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/sign-in").toExternalForm,
        credentials
      )
      .flatMap { r =>
        (r.status, r.header(HeaderNames.LOCATION)) match {
          case (Status.BAD_REQUEST, _) => throw new BadRequestException(s"$baseUrl/agents-external-stubs/sign-in")
          case (_, None)               => throw new IllegalStateException()
          case (s, Some(l)) =>
            http
              .GET[AuthenticatedSession](new URL(s"$baseUrl$l").toExternalForm)
              .map(_.copy(newUserCreated = Some(s == 201)))
        }
      }

  def signOut()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .GET[HttpResponse](new URL(s"$baseUrl/agents-external-stubs/sign-out").toExternalForm)
      .map(_ => ())

  def currentSession()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] =
    http
      .GET[AuthenticatedSession](new URL(s"$baseUrl/agents-external-stubs/session/current").toExternalForm)

  def getUser(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[User] =
    http.GET[User](new URL(s"$baseUrl/agents-external-stubs/users/$userId").toExternalForm).recover(handleNotFound)

  def createUser(user: User, affinityGroup: Option[String])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] =
    http
      .POST[User, HttpResponse](
        new URL(
          s"$baseUrl/agents-external-stubs/users" + affinityGroup.fold("")(ag => s"?affinityGroup=$ag")
        ).toExternalForm,
        user
      )
      .recover(handleNotFound)
      .map(_ => ())

  def updateUser(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[User, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/users/${user.userId}").toExternalForm,
        user
      )
      .recover(handleNotFound)
      .map(_ => ())

  def getUsers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Users] =
    http.GET[Users](new URL(s"$baseUrl/agents-external-stubs/users").toExternalForm)

  def getUsersByGroupId(groupId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Users] =
    http.GET[Users](new URL(s"$baseUrl/agents-external-stubs/users?groupId=$groupId").toExternalForm)

  def removeUser(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/users/$userId").toExternalForm
      )
      .recover(handleNotFound)
      .map(_ => ())

  def getGroup(groupId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Group] =
    http.GET[Group](new URL(s"$baseUrl/agents-external-stubs/groups/$groupId").toExternalForm).recover(handleNotFound)

  def getGroups(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Groups] =
    http.GET[Groups](new URL(s"$baseUrl/agents-external-stubs/groups").toExternalForm)

  def updateGroup(group: Group)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[Group, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/groups/${group.groupId}").toExternalForm,
        group
      )
      .recover(handleNotFound)
      .map(_ => ())

  def massCreateAssistantsAndUsers(
    request: GranPermsGenRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GranPermsGenResponse] =
    httpV2
      .post(new URL(s"$baseUrl/agents-external-stubs/test/gran-perms/generate-users"))
      .withBody(Json.toJson(request))
      .transform(ws => ws.withRequestTimeout(2.minute))
      .execute[GranPermsGenResponse]
      .recover(handleNotFound)

  def getRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Records] =
    http.GET[Records](new URL(s"$baseUrl/agents-external-stubs/records").toExternalForm).recover(handleNotFound)

  def getRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsObject] =
    http.GET[JsObject](new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm).recover(handleNotFound)

  def generateRecord(recordType: String, seed: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[JsObject] =
    http
      .GET[JsObject](
        new URL(s"$baseUrl/agents-external-stubs/records/$recordType/generate?seed=$seed&minimal=false").toExternalForm
      )
      .recover(handleNotFound)

  def updateRecord(id: String, record: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[JsValue, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm,
        record.+("id" -> JsString(id))
      )
      .recover(handleNotFound)
      .map(_ => ())

  def deleteRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm
      )
      .recover(handleNotFound)
      .map(_ => ())

  def createRecord(recordType: String, record: JsObject)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[String]] =
    http
      .POST[JsValue, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/records/$recordType").toExternalForm,
        record
      )
      .map(r =>
        r.status match {
          case Status.CREATED => (r.json \ "_links" \ 0 \ "href").asOpt[String].map(_.split("/").last)
          case s              => throw new RuntimeException(s"unexpected error, status: $s")
        }
      )

  def getKnownFacts(enrolmentKey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EnrolmentInfo] =
    http
      .GET[EnrolmentInfo](new URL(s"$baseUrl/agents-external-stubs/known-facts/$enrolmentKey").toExternalForm)
      .recover(handleNotFound)

  def getServicesInfo()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Services] =
    http
      .GET[Services](new URL(s"$baseUrl/agents-external-stubs/config/services").toExternalForm)
      .map(s => s.copy(s.services.sortBy(_.name)))
      .recover(handleNotFound)

  def destroyPlanet(planetId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/planets/$planetId").toExternalForm
      )
      .recover(handleNotFound)
      .map(_ => ())

  def getAllSpecialCases(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SpecialCase]] =
    http
      .GET[Option[Seq[SpecialCase]]](new URL(s"$baseUrl/agents-external-stubs/special-cases").toExternalForm)
      .map {
        case Some(seq) => seq
        case None      => Seq.empty
      }
      .recover { case _ =>
        Seq.empty
      }

  def getSpecialCase(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SpecialCase]] =
    http
      .GET[SpecialCase](new URL(s"$baseUrl/agents-external-stubs/special-cases/$id").toExternalForm)
      .map(Option.apply)
      .recover { case _: NotFoundException =>
        None
      }
      .recover(handleNotFound)

  def createSpecialCase(specialCase: SpecialCase)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    http
      .POST[SpecialCase, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/special-cases").toExternalForm,
        specialCase
      )
      .map(r =>
        r.header(HeaderNames.LOCATION)
          .map(_.split("/").last)
          .getOrElse(throw new Exception("Missing location header in the response"))
      )
      .recover(handleNotFound)

  def updateSpecialCase(specialCase: SpecialCase)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    http
      .PUT[SpecialCase, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/special-cases/${specialCase.id.get}").toExternalForm,
        specialCase
      )
      .map(r =>
        r.header(HeaderNames.LOCATION)
          .map(_.split("/").last)
          .getOrElse(throw new Exception("Missing location header in the response"))
      )
      .recover(handleNotFound)

  def deleteSpecialCase(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/special-cases/$id").toExternalForm
      )
      .recover(handleNotFound)
      .map(_ => ())

  def storePdvResult(id: String, success: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .POSTEmpty[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/pdv-result/$id/$success").toExternalForm
      )
      .recover(handleNotFound)
      .map(_ => ())

  private def handleNotFound[U]: PartialFunction[Throwable, U] = {
    case UpstreamErrorResponse(message, upstreamResponseCode, _, _) if upstreamResponseCode == Status.NOT_FOUND =>
      throw new NotFoundException(message)
    case e => throw e
  }
}
