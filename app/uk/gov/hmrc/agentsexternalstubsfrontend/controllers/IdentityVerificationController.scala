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

package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.net.URLDecoder
import java.util.UUID
import com.google.inject.Provider

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html.iv_uplift
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationController @Inject() (
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  ivUpliftView: iv_uplift,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthActions with I18nSupport {

  implicit val ec: ExecutionContext = ecp.get

  import IdentityVerificationController._

  val journeyIdValue: String = UUID.randomUUID().toString

  val options: String => Seq[(String, String)] = (journeyId: String) =>
    Seq(
      s"Success~$journeyId"              -> "Success",
      s"Incomplete~$journeyId"           -> "Incomplete",
      s"FailedDirectorCheck~$journeyId"  -> "FailedDirectorCheck",
      s"PreconditionFailed~$journeyId"   -> "Precondition Failed",
      s"LockedOut~$journeyId"            -> "Locked Out",
      s"InsufficientEvidence~$journeyId" -> "Insufficient Evidence",
      s"FailedMatching~$journeyId"       -> "Failed Matching",
      s"TechnicalIssue~$journeyId"       -> "Technical Issue",
      s"UserAborted~$journeyId"          -> "User Aborted",
      s"TimedOut~$journeyId"             -> "Timed Out",
      s"FailedIV~$journeyId"             -> "Failed IV"
    )

  private val upliftUrl = (
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String],
    doProxy: Boolean
  ) =>
    if (doProxy)
      routes.IdentityVerificationController
        .upliftProxy(journeyIdValue, confidenceLevel, completionURL, failureURL, origin)
    else
      routes.IdentityVerificationController
        .upliftInternal(journeyIdValue, confidenceLevel, completionURL, failureURL, origin)

  private def showUpliftPage(
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  )(doProxy: Boolean): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector.getUser(credentials.providerId).map { currentUser =>
          val nino: String = currentUser.nino.map(_.value).getOrElse("")
          Ok(
            ivUpliftView(
              UpliftRequestForm.fill(UpliftRequest(nino, "")),
              options(journeyIdValue),
              upliftUrl(confidenceLevel, completionURL, failureURL, origin, doProxy)
            )
          )
        }
      }
    }

  private def redirectWithJourneyId(targetUrl: String, journeyId: String): Result = {
    val cleanUrl = URLDecoder.decode(targetUrl, "UTF-8")
    val tokenAppendChar = if (cleanUrl.contains("?")) "&" else "?"
    Redirect(s"$cleanUrl${tokenAppendChar}journeyId=$journeyId")
  }

  def showUpliftPageProxy(
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  ): Action[AnyContent] =
    showUpliftPage(confidenceLevel, completionURL, failureURL, origin)(true)

  def showUpliftPageInternal(
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  ): Action[AnyContent] =
    showUpliftPage(confidenceLevel, completionURL, failureURL, origin)(false)

  private def uplift(
    journeyId: String,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  )(doProxy: Boolean): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    ivUpliftView(
                      formWithErrors,
                      options(journeyId),
                      upliftUrl(250, completionURL, failureURL, origin, doProxy)
                    )
                  )
                ),
              upliftRequest => {
                val journeyIdMatches = upliftRequest.option.contains(journeyId)
                val isSuccessful = upliftRequest.option.contains("Success")
                if (journeyIdMatches && isSuccessful) {
                  for {
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(250))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield redirectWithJourneyId(completionURL.unsafeValue, journeyId)
                } else {
                  storeResult((journeyId, upliftRequest.option.split('~').head))
                  Future.successful(redirectWithJourneyId(failureURL.unsafeValue, journeyId))
                }
              }
            )
        }
    }

  var identityVerificationResults = scala.collection.mutable.Map[String, String]()

  private def storeResult(pair: (String, String)): Unit =
    identityVerificationResults += (pair._1 -> pair._2)

  private def getResult(journeyId: String): Option[String] =
    identityVerificationResults.get(journeyId)

  def upliftProxy(
    journeyId: String,
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  ): Action[AnyContent] =
    uplift(journeyId, completionURL, failureURL, origin)(true)

  def upliftInternal(
    journeyId: String,
    confidenceLevel: Int,
    completionURL: RedirectUrl,
    failureURL: RedirectUrl,
    origin: Option[String]
  ): Action[AnyContent] =
    uplift(journeyId, completionURL, failureURL, origin)(false)

  private def getIvResult(journeyId: String) = Action.async {
    if (journeyId.nonEmpty) {
      try {
        val result = getResult(journeyId).get
        Future.successful(Ok(Json.obj("token" -> journeyId, "result" -> result)))
      } catch {
        case _: NoSuchElementException => Future.successful(NotFound)
      } finally identityVerificationResults.clear()
    } else {
      Future.successful(NotFound)
    }
  }

  def getIvResultProxy(journeyIdAndReason: String): Action[AnyContent] = getIvResult(journeyIdAndReason)

  def getIvResultInternal(journeyIdAndReason: String): Action[AnyContent] = getIvResult(journeyIdAndReason)
}

object IdentityVerificationController {

  case class UpliftRequest(nino: String, option: String)

  object UpliftRequest {
    implicit val format: OFormat[UpliftRequest] = Json.format[UpliftRequest]
  }

  val UpliftRequestForm: Form[UpliftRequest] = Form[UpliftRequest](
    mapping(
      "nino"   -> text.verifying("Enter a valid Nino", v => Nino.isValid(v.replace(" ", ""))),
      "option" -> text.verifying("Please select an Option", _.nonEmpty)
    )((nino, option) => UpliftRequest(nino.replace(" ", ""), option))(UpliftRequest.unapply)
  )
}
