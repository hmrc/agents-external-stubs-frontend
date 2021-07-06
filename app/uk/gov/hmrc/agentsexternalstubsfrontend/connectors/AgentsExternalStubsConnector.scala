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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import play.api.http.Status

import java.net.URL
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

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
class AgentsExternalStubsConnector @Inject() (appConfig: FrontendConfig, http: HttpClient) {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  val baseUrl = appConfig.aesBaseUrl

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
    http.GET[User](new URL(s"$baseUrl/agents-external-stubs/users/$userId").toExternalForm).recover(handleGetResponse)

  def createUser(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .POST[User, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/users").toExternalForm,
        user
      ) map handleNoContentResponse

  def updateUser(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[User, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/users/${user.userId}").toExternalForm,
        user
      ) map handleNoContentResponse

  def getUsers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Users] =
    http.GET[Users](new URL(s"$baseUrl/agents-external-stubs/users").toExternalForm)

  def removeUser(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/users/$userId").toExternalForm
      ) map handleNoContentResponse

  def getRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Records] =
    http.GET[Records](new URL(s"$baseUrl/agents-external-stubs/records").toExternalForm).recover(handleGetResponse)

  def getRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsObject] =
    http.GET[JsObject](new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm).recover(handleGetResponse)

  def generateRecord(recordType: String, seed: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[JsObject] =
    http
      .GET[JsObject](
        new URL(s"$baseUrl/agents-external-stubs/records/$recordType/generate?seed=$seed&minimal=false").toExternalForm
      )
      .recover(handleGetResponse)

  def updateRecord(id: String, record: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[JsValue, HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm,
        record.+("id" -> JsString(id))
      ) map handleNoContentResponse

  def deleteRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/records/$id").toExternalForm
      ) map handleNoContentResponse

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
          case Status.NO_CONTENT => (r.json \ "_links" \ 0 \ "href").asOpt[String].map(_.split("/").last)
          case s                 => throw new RuntimeException(s"unexpected error, status: $s")
        }
      )

  def getKnownFacts(enrolmentKey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EnrolmentInfo] =
    http
      .GET[EnrolmentInfo](new URL(s"$baseUrl/agents-external-stubs/known-facts/$enrolmentKey").toExternalForm)
      .recover(handleGetResponse)

  def getServicesInfo()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Services] =
    http
      .GET[Services](new URL(s"$baseUrl/agents-external-stubs/config/services").toExternalForm)
      .map(s => s.copy(s.services.sortBy(_.name)))
      .recover(handleGetResponse)

  def destroyPlanet(planetId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/planets/$planetId").toExternalForm
      ) map handleNoContentResponse

  def getAllSpecialCases(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SpecialCase]] =
    http
      .GET[Option[Seq[SpecialCase]]](new URL(s"$baseUrl/agents-external-stubs/special-cases").toExternalForm)
      .map {
        case Some(seq) => seq
        case None      => Seq.empty
      }
      .recover(handleGetResponse)

  def getSpecialCase(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SpecialCase]] =
    http
      .GET[SpecialCase](new URL(s"$baseUrl/agents-external-stubs/special-cases/$id").toExternalForm)
      .map(Option.apply)
      .recover { case _: NotFoundException =>
        None
      }
      .recover(handleGetResponse)

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
      .recover { case Upstream4xxException(e) =>
        throw e
      }

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
      .recover { case Upstream4xxException(e) =>
        throw e
      }

  def deleteSpecialCase(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/special-cases/$id").toExternalForm
      ) map handleNoContentResponse

  def storePdvResult(id: String, success: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .POSTEmpty[HttpResponse](
        new URL(s"$baseUrl/agents-external-stubs/pdv-result/$id/$success").toExternalForm
      ) map handleNoContentResponse

  // This replicates existing behaviour
  private def handleNoContentResponse(r: HttpResponse): Unit =
    r.status match {
      case Status.NO_CONTENT => ()
      case Status.OK         => ()
      case Status.ACCEPTED   => ()
      case Status.NOT_FOUND  => ()
      case s                 => throw new RuntimeException(s"unexpected error, status: $s")
    }

  private def handleGetResponse[U]: PartialFunction[Throwable, U] = {
    case Upstream4xxResponse(message, upstreamResponseCode, _, _) if upstreamResponseCode == Status.NOT_FOUND =>
      throw new NotFoundException(message)
  }
}
