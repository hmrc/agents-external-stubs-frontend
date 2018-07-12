package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.LoginController.Credentials
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedSession(sessionId: String)

object AuthenticatedSession {
  implicit val reads: Reads[AuthenticatedSession] = Json.reads[AuthenticatedSession]
}

@Singleton
class AgentsExternalStubsConnector @Inject()(
  @Named("agents-external-stubs-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost) {

  def login(credentials: Credentials)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthenticatedSession] =
    http
      .POST[Credentials, HttpResponse](new URL(baseUrl, "/agents-external-stubs/login").toExternalForm, credentials)
      .map(
        response =>
          response
            .header(HeaderNames.LOCATION)
            .getOrElse(throw new IllegalStateException()))
      .flatMap(location => http.GET[AuthenticatedSession](new URL(baseUrl, location).toExternalForm))

}
