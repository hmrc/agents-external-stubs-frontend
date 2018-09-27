package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.nio.charset.StandardCharsets
import java.util.Base64

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RestQueryController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  wsClient: WSClient
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  import RestQueryController._

  def showRestQueryPage(q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentials) { credentials =>
        q match {
          case None =>
            Future.successful(
              Ok(html.rest_query(
                RestQueryForm.fill(RestQuery("GET", "https://", None)),
                routes.RestQueryController.runQuery(),
                routes.UserController.showUserPage(),
                routes.RestQueryController.showRestQueryPage(None),
                None,
                credentials.providerId,
                None
              )))
          case Some(query) =>
            RestQuery
              .decode(query)
              .fold(
                errors => Future.successful(BadRequest(errors)),
                rq =>
                  runQuery(rq).map(response =>
                    Ok(html.rest_query(
                      RestQueryForm.fill(rq),
                      routes.RestQueryController.runQuery(),
                      routes.UserController.showUserPage(),
                      routes.RestQueryController.showRestQueryPage(None),
                      Option(response),
                      credentials.providerId,
                      Option(rq.toCurlCommand)
                    )))
              )

        }
      }
  }

  val runQuery: Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentials) { credentials =>
        RestQueryForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(Ok(html.rest_query(
                formWithErrors,
                routes.RestQueryController.runQuery(),
                routes.UserController.showUserPage(),
                routes.RestQueryController.showRestQueryPage(None),
                None,
                credentials.providerId,
                None
              ))),
            query => Future.successful(Redirect(routes.RestQueryController.showRestQueryPage(Some(query.encode))))
          )
      }
  }

  private def runQuery(
    query: RestQuery)(implicit ec: ExecutionContext, request: Request[AnyContent]): Future[WSResponse] = {
    val wsRequest = wsClient
      .url(query.url)
      .withHeaders(
        HeaderNames.AUTHORIZATION -> request.session.get(SessionKeys.authToken).get,
        "X-Session-ID"            -> request.session.get(SessionKeys.sessionId).get)
    query.method match {
      case "GET"    => wsRequest.get()
      case "POST"   => wsRequest.post(query.payload.getOrElse(JsNull))
      case "PUT"    => wsRequest.put(query.payload.getOrElse(JsNull))
      case "DELETE" => wsRequest.delete()
      case _        => Future.failed(new Exception(s"Method ${query.method} is not supported, try GET, POST, PUT or DELETE"))
    }
  }

}

object RestQueryController {

  case class RestQuery(method: String, url: String, payload: Option[JsValue]) {
    def encode: String =
      new String(
        Base64.getUrlEncoder.encode(Json.toJson(this).toString().getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8)

    def toCurlCommand(implicit request: Request[AnyContent]): String =
      s"""curl -v -X $method -H "Authorization: ${request.session
        .get(SessionKeys.authToken)
        .get}" -H "X-Session-ID: ${request.session
        .get(SessionKeys.sessionId)
        .get}" ${if (method == "POST" || method == "PUT") """-H "Content-Type: application/json"""" else ""} ${payload
        .map(p => s"""--data '$p'""")
        .getOrElse("")} $url"""
  }

  object RestQuery {
    implicit val format: Format[RestQuery] = Json.format[RestQuery]

    def decode(s: String): Either[String, RestQuery] =
      try {
        Right(Json.parse(new String(Base64.getUrlDecoder.decode(s), StandardCharsets.UTF_8)).as[RestQuery])
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }
  }

  val validJson: Constraint[Option[String]] = Constraint {
    case Some(json) => try { Json.parse(json); Valid } catch { case NonFatal(e) => Invalid(e.getMessage) }
    case None       => Valid
  }

  val RestQueryForm: Form[RestQuery] = Form[RestQuery](
    mapping(
      "method" -> text,
      "url"    -> text,
      "payload" -> optional(text)
        .verifying(validJson)
        .transform[Option[JsValue]](_.map(Json.parse), _.map(Json.prettyPrint))
    )(RestQuery.apply)(RestQuery.unapply))

}
