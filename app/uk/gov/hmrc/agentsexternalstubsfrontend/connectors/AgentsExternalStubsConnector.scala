package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.User
import uk.gov.hmrc.http.{BadRequestException, _}

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedSession(
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
  http: HttpGet with HttpPost with HttpPut) {

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
        case e: BadRequestException =>
          val m = e.getMessage
          val p = ",\"message\":\""
          val s = m.indexOf(p) + p.length
          val b = if (s > 0) m.substring(s, m.length - 3) else m
          throw new BadRequestException(b)
      }

}
