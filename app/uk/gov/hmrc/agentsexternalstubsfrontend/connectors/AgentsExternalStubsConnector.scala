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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import play.api.http.Status
import play.api.libs.json.*
import play.mvc.Http.HeaderNames
import play.api.libs.ws.writeableOf_JsValue
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URI
import javax.inject.{Inject, Singleton}
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
  given Reads[AuthenticatedSession] = Json.reads[AuthenticatedSession]
}

@Singleton
class AgentsExternalStubsConnector @Inject() (appConfig: FrontendConfig, http: HttpClientV2) {

  val baseUrl: String = appConfig.aesBaseUrl

  def signIn()(using hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/sign-in"
    http
      .post(requestUrl)
      .execute[HttpResponse]
      .flatMap { r =>
        (r.status, r.header(HeaderNames.LOCATION)) match {
          case (Status.BAD_REQUEST, _) => throw new BadRequestException(s"$baseUrl/agents-external-stubs/sign-in")
          case (_, None)               => throw new IllegalStateException()
          case (s, Some(l)) =>
            val urlBuilder = baseUrl + l
            val getSessionUrl = url"$urlBuilder"
            http
              .get(getSessionUrl)
              .execute[AuthenticatedSession]
              .map(_.copy(newUserCreated = Some(s == 201)))
        }
      }
  }

  def signIn(
    credentials: SignInRequest
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/sign-in"
    http
      .post(requestUrl)
      .withBody(Json.toJson(credentials))
      .execute[HttpResponse]
      .flatMap { r =>
        (r.status, r.header(HeaderNames.LOCATION)) match {
          case (Status.BAD_REQUEST, _) => throw new BadRequestException(s"$baseUrl/agents-external-stubs/sign-in")
          case (_, None)               => throw new IllegalStateException()
          case (s, Some(l)) =>
            val urlBuilder = baseUrl + l
            val getSessionUrl = url"$urlBuilder"
            http
              .get(getSessionUrl)
              .execute[AuthenticatedSession]
              .map(_.copy(newUserCreated = Some(s == 201)))
        }
      }
  }

  def signOut()(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/sign-out"
    http
      .get(requestUrl)
      .execute[HttpResponse]
      .map(_ => ())
  }

  def currentSession()(using hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/session/current"
    http
      .get(requestUrl)
      .execute[AuthenticatedSession]
  }

  def getUser(userId: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[User] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/users/$userId"
    http
      .get(requestUrl)
      .execute[User]
      .recover(handleNotFound)
  }

  def createUser(affinityGroup: Option[AffinityGroup], userBody: JsValue)(using
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[User] = {
    val urlBuilder =
      s"$baseUrl/agents-external-stubs/users" + affinityGroup.fold("")(ag => s"?affinityGroup=${ag.toString}")
    val requestUrl = url"$urlBuilder"
    http
      .post(requestUrl)
      .withBody(userBody)
      .execute[HttpResponse]
      .flatMap { r =>
        (r.status, r.header(HeaderNames.LOCATION)) match {
          case (Status.BAD_REQUEST, _) => throw new BadRequestException(s"$baseUrl/agents-external-stubs/sign-in")
          case (_, None)               => throw new IllegalStateException()
          case (_, Some(l)) =>
            val urlBuilder = baseUrl + l
            val getUserUrl = url"$urlBuilder"
            http
              .get(getUserUrl)
              .execute[HttpResponse]
              .map(res => res.json.as[User])
        }
      }
  }

  def createUser(user: User, affinityGroup: Option[String])(using
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val urlBuilder = s"$baseUrl/agents-external-stubs/users" + affinityGroup.fold("")(ag => s"?affinityGroup=$ag")
    val requestUrl = url"$urlBuilder"
    http
      .post(requestUrl)
      .withBody(Json.toJson(user))
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def updateUser(user: User)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/users/${user.userId}"
    http
      .put(requestUrl)
      .withBody(Json.toJson(user))
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def getUsers(
    userId: Option[String] = None,
    groupId: Option[String] = None,
    principalEnrolmentService: Option[String] = None,
    limit: Option[Int] = None
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[Users] = {

    val params: List[(String, String)] = List(
      limit.map(_.toString).map(("limit", _)),
      userId.map(("userId", _)),
      groupId.map(("groupId", _)),
      principalEnrolmentService.map(("principalEnrolmentService", _))
    ).flatten

    val uri = uri"/users".addParams(params*)
    val requestUrl = new URI(s"$baseUrl/agents-external-stubs$uri").toURL

    http
      .get(requestUrl)
      .execute[Users]
  }

  def removeUser(userId: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/users/$userId"
    http
      .delete(requestUrl)
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def getGroup(groupId: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Group] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/groups/$groupId"
    http
      .get(requestUrl)
      .execute[Group]
      .recover(handleNotFound)
  }

  def getGroups(using hc: HeaderCarrier, ec: ExecutionContext): Future[Groups] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/groups"
    http
      .get(requestUrl)
      .execute[Groups]
  }

  def updateGroup(group: Group)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/groups/${group.groupId}"
    http
      .put(requestUrl)
      .withBody(Json.toJson(group))
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def massCreateAssistantsAndUsers(
    request: GranPermsGenRequest
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[GranPermsGenResponse] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/test/gran-perms/generate-users"
    http
      .post(requestUrl)
      .withBody(Json.toJson(request))
      .transform(ws => ws.withRequestTimeout(2.minute))
      .execute[GranPermsGenResponse]
      .recover(handleNotFound)
  }

  def getRecords(using hc: HeaderCarrier, ec: ExecutionContext): Future[Records] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records"
    http
      .get(requestUrl)
      .execute[Records]
      .recover(handleNotFound)
  }

  def getRecord(id: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[JsObject] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records/$id"
    http
      .get(requestUrl)
      .execute[JsObject]
      .recover(handleNotFound)
  }

  def generateRecord(recordType: String, seed: String)(using
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[JsObject] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records/$recordType/generate?seed=$seed&minimal=false"
    http
      .get(requestUrl)
      .execute[JsObject]
      .recover(handleNotFound)
  }

  def updateRecord(id: String, record: JsObject)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records/$id"
    http
      .put(requestUrl)
      .withBody(Json.toJson(record.+("id" -> JsString(id))))
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def deleteRecord(id: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records/$id"
    http
      .delete(requestUrl)
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def createRecord(recordType: String, record: JsObject)(using
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[String]] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/records/$recordType"
    http
      .post(requestUrl)
      .withBody(Json.toJson(record))
      .execute[HttpResponse]
      .map(r =>
        r.status match {
          case Status.CREATED => (r.json \ "_links" \ 0 \ "href").asOpt[String].map(_.split("/").last)
          case s              => throw new RuntimeException(s"unexpected error, status: $s")
        }
      )
  }

  def getKnownFacts(enrolmentKey: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[EnrolmentInfo] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/known-facts/$enrolmentKey"
    http
      .get(requestUrl)
      .execute[EnrolmentInfo]
      .recover(handleNotFound)
  }

  def getServicesInfo()(using hc: HeaderCarrier, ec: ExecutionContext): Future[Services] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/config/services"
    http
      .get(requestUrl)
      .execute[Services]
      .map(s => s.copy(s.services.sortBy(_.name)))
      .recover(handleNotFound)
  }

  def destroyPlanet(planetId: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/planets/$planetId"
    http
      .delete(requestUrl)
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def getAllSpecialCases(using hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SpecialCase]] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/special-cases"
    http
      .get(requestUrl)
      .execute[Option[Seq[SpecialCase]]]
      .map {
        case Some(seq) => seq
        case None      => Seq.empty
      }
      .recover { case _ =>
        Seq.empty
      }
  }

  def getSpecialCase(id: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SpecialCase]] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/special-cases/$id"
    http
      .get(requestUrl)
      .execute[SpecialCase]
      .map(Option.apply)
      .recover { case _: NotFoundException =>
        None
      }
      .recover(handleNotFound)
  }

  def createSpecialCase(specialCase: SpecialCase)(using hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/special-cases"
    http
      .post(requestUrl)
      .withBody(Json.toJson(specialCase))
      .execute[HttpResponse]
      .map(r =>
        r.header(HeaderNames.LOCATION)
          .map(_.split("/").last)
          .getOrElse(throw new Exception("Missing location header in the response"))
      )
      .recover(handleNotFound)
  }

  def updateSpecialCase(specialCase: SpecialCase)(using hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/special-cases/${specialCase.id.get}"
    http
      .put(requestUrl)
      .withBody(Json.toJson(specialCase))
      .execute[HttpResponse]
      .map(r =>
        r.header(HeaderNames.LOCATION)
          .map(_.split("/").last)
          .getOrElse(throw new Exception("Missing location header in the response"))
      )
      .recover(handleNotFound)
  }

  def deleteSpecialCase(id: String)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/special-cases/$id"
    http
      .delete(requestUrl)
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  def storePdvResult(id: String, success: Boolean)(using hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val requestUrl = url"$baseUrl/agents-external-stubs/pdv-result/$id/$success"
    http
      .post(requestUrl)
      .execute[HttpResponse]
      .recover(handleNotFound)
      .map(_ => ())
  }

  private def handleNotFound[U]: PartialFunction[Throwable, U] = {
    case UpstreamErrorResponse(message, upstreamResponseCode, _, _) if upstreamResponseCode == Status.NOT_FOUND =>
      throw new NotFoundException(message)
    case e => throw e
  }
}
