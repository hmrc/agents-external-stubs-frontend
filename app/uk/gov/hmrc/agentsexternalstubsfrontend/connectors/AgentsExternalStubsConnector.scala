package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Records, User, Users}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedSession(
  sessionId: String,
  userId: String,
  authToken: String,
  providerType: String,
  planetId: String,
  newUserCreated: Option[Boolean] = None)

object AuthenticatedSession {
  implicit val reads: Reads[AuthenticatedSession] = Json.reads[AuthenticatedSession]
}

@Singleton
class AgentsExternalStubsConnector @Inject()(
  @Named("agents-external-stubs-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost with HttpPut with HttpDelete) {

  def signIn(
    credentials: SignInRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] =
    http
      .POST[SignInRequest, HttpResponse](new URL(baseUrl, "/agents-external-stubs/sign-in").toExternalForm, credentials)
      .map(
        response =>
          (
            response
              .header(HeaderNames.LOCATION)
              .getOrElse(throw new IllegalStateException()),
            response.status))
      .flatMap {
        case (location, status) =>
          http
            .GET[AuthenticatedSession](new URL(baseUrl, location).toExternalForm)
            .map(_.copy(newUserCreated = Some(status == 201)))
      }

  def signOut()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .GET[HttpResponse](new URL(baseUrl, "/agents-external-stubs/sign-out").toExternalForm)
      .map(_ => ())

  def getUser(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[User] =
    http.GET[User](new URL(baseUrl, s"/agents-external-stubs/users/$userId").toExternalForm)

  def updateUser(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[User, HttpResponse](new URL(baseUrl, s"/agents-external-stubs/users/${user.userId}").toExternalForm, user)
      .map(_ => ())
      .recover {
        case Upstream4xxException(e) => throw e
      }

  def getUsers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Users] =
    http.GET[Users](new URL(baseUrl, s"/agents-external-stubs/users").toExternalForm)

  def removeUser(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](new URL(baseUrl, s"/agents-external-stubs/users/$userId").toExternalForm)
      .map(_ => ())
      .recover {
        case Upstream4xxException(e) => throw e
      }

  def getRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Records] =
    http.GET[Records](new URL(baseUrl, s"/agents-external-stubs/records").toExternalForm)

  def getRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsObject] =
    http.GET[JsObject](new URL(baseUrl, s"/agents-external-stubs/records/$id").toExternalForm)

  def generateRecord(recordType: String, seed: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[JsObject] =
    http.GET[JsObject](
      new URL(baseUrl, s"/agents-external-stubs/records/$recordType/generate?seed=$seed&minimal=false").toExternalForm)

  def updateRecord(id: String, record: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .PUT[JsValue, HttpResponse](
        new URL(baseUrl, s"/agents-external-stubs/records/$id").toExternalForm,
        record.+("id" -> JsString(id))
      )
      .map(_ => ())
      .recover {
        case Upstream4xxException(e) => throw e
      }

  def deleteRecord(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http
      .DELETE[HttpResponse](new URL(baseUrl, s"/agents-external-stubs/records/$id").toExternalForm)
      .map(_ => ())
      .recover {
        case Upstream4xxException(e) => throw e
      }

  def createRecord(recordType: String, record: JsObject)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Unit] =
    http
      .POST[JsValue, HttpResponse](
        new URL(baseUrl, s"/agents-external-stubs/records/$recordType").toExternalForm,
        record
      )
      .map(_ => ())
      .recover {
        case Upstream4xxException(e) => throw e
      }

}
