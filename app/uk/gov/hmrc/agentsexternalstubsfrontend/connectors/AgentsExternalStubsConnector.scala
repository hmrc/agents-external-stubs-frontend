package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.SignInController.SignInRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedSession(
  userId: String,
  authToken: String,
  providerType: String,
  newUserCreated: Option[Boolean] = None)

object AuthenticatedSession {
  implicit val reads: Reads[AuthenticatedSession] = Json.reads[AuthenticatedSession]
}

@Singleton
class AgentsExternalStubsConnector @Inject()(
  @Named("agents-external-stubs-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost) {

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

}
