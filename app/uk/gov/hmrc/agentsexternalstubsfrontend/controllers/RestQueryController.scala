/*
 * Copyright 2020 HM Revenue & Customs
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

import java.nio.charset.StandardCharsets
import java.util.Base64

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text, seq, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.rest_query
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.Try
import java.net.URL

@Singleton
class RestQueryController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val features: Features,
  val wsClient: WSClient,
  restQueryView: rest_query,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  import RestQueryController._

  def showRestQueryPage(q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        if (features.mayShowRestQuery(credentials.planetId))
          q match {
            case None =>
              Future.successful(
                Ok(restQueryView(
                  RestQueryForm.fill(RestQuery("GET", "https://", None, None)),
                  routes.RestQueryController.runQuery(),
                  routes.UserController.showUserPage(),
                  routes.RestQueryController.showRestQueryPage(None),
                  None,
                  None,
                  pageContext(credentials)
                )))
            case Some(query) =>
              RestQuery
                .decode(query)
                .fold(
                  errors => Future.successful(BadRequest(errors)),
                  rq =>
                    runQuery(rq)
                      .map(response =>
                        Ok(restQueryView(
                          RestQueryForm.fill(rq),
                          routes.RestQueryController.runQuery(),
                          routes.UserController.showUserPage(),
                          routes.RestQueryController.showRestQueryPage(None),
                          Option(response),
                          Option(rq.toCurlCommand),
                          pageContext(credentials)
                        )))
                      .recover {
                        case NonFatal(e) =>
                          Ok(restQueryView(
                            RestQueryForm.fill(rq).withGlobalError(e.getMessage),
                            routes.RestQueryController.runQuery(),
                            routes.UserController.showUserPage(),
                            routes.RestQueryController.showRestQueryPage(None),
                            None,
                            Option(rq.toCurlCommand),
                            pageContext(credentials)
                          ))
                    }
                )

          } else Future.successful(Forbidden)
      }

  }

  val runQuery: Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        if (features.mayShowRestQuery(credentials.planetId))
          RestQueryForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(Ok(restQueryView(
                  formWithErrors,
                  routes.RestQueryController.runQuery(),
                  routes.UserController.showUserPage(),
                  routes.RestQueryController.showRestQueryPage(None),
                  None,
                  None,
                  pageContext(credentials)
                ))),
              query => Future.successful(Redirect(routes.RestQueryController.showRestQueryPage(Some(query.encode))))
            )
        else Future.successful(Forbidden)
      }
  }

  private def runQuery(
    query: RestQuery)(implicit ec: ExecutionContext, request: Request[AnyContent]): Future[WSResponse] = {
    val headers = Map(
        HeaderNames.AUTHORIZATION -> request.session.get(SessionKeys.authToken).get,
        "X-Session-ID"            -> request.session.get(SessionKeys.sessionId).get) ++ 
        query.headers.map(RestQueryController.parseHeaders).getOrElse(Map.empty)  
    if (Try(new URL(query.url)).isFailure) 
      Future.failed(new Exception(s"Invalid URL ${query.url}"))
    else {
      Try{
          val wsRequest = wsClient
          .url(query.url)
          .withHttpHeaders(headers.toSeq:_*)
        query.method match {
          case "GET"    => wsRequest.get()
          case "POST"   => wsRequest.post(query.payload.getOrElse(JsNull))
          case "PUT"    => wsRequest.put(query.payload.getOrElse(JsNull))
          case "DELETE" => wsRequest.delete()
          case _        => Future.failed(new Exception(s"Method ${query.method} is not supported, try GET, POST, PUT or DELETE"))
        }
      }.fold(
        e => Future.failed(new Exception(s"Error executing the request: ${e.getMessage}")),
        identity
      )
    }
  }

}

object RestQueryController {

  case class RestQuery(method: String, url: String, payload: Option[JsValue], headers: Option[String]) {
    def encode: String =
      new String(
        Base64.getUrlEncoder.encode(Json.toJson(this).toString().getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8)

    def toCurlCommand(implicit request: Request[AnyContent]): String = {
      val h = Map(
        "Authorization"->request.session.get(SessionKeys.authToken).get,
        "X-Session-ID" -> request.session.get(SessionKeys.sessionId).get
      ) ++ headers.map(RestQueryController.parseHeaders).getOrElse(Map.empty)

      s"""curl -v -X $method ${h.map{case (key,value) => s"""-H "$key: $value""""}.mkString(" ")} ${if (method == "POST" || method == "PUT") """-H "Content-Type: application/json"""" else ""} ${payload
        .map(p => s"""--data '$p'""")
        .getOrElse("")} $url"""
    }
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
      "url"    -> text.verifying("error.url", u => Try(new URL(u)).isSuccess),
      "payload" -> optional(text)
        .verifying(validJson)
        .transform[Option[JsValue]](_.map(Json.parse), _.map(Json.prettyPrint)),
      "headers" -> optional(text).verifying("error.headers", _.forall{ h => parseHeaders(h); true})
    )(RestQuery.apply)(RestQuery.unapply))

  def prettyPrintHeaders(headers: Seq[(String,String)]): Option[String] = 
    if (headers.isEmpty) None 
    else 
      Some( headers.map { 
        case (key, value) => s"$key: $value"
      }.mkString("\n"))

  def parseHeaders(headers: String): Map[String,String] = 
    if (headers.isEmpty) Map.empty 
    else 
      headers
        .lines
        .map { line => 
          val key = line.takeWhile(_ != ':')
          if(key.nonEmpty) {
            val value = line.drop(key.length + 1)
            if(value.nonEmpty) {
              Some((key.trim(), value.trim()))
            } else None
          } else None
        }
        .collect {case Some(x) => x}
        .toMap

}
