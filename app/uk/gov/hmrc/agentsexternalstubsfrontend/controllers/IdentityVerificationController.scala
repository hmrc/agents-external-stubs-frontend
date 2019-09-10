package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.net.URLDecoder
import java.util.UUID

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationController @Inject()(
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  ecp: Provider[ExecutionContext])(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  implicit val ec: ExecutionContext = ecp.get

  import IdentityVerificationController._

  val journeyIdValue: String = UUID.randomUUID().toString

  val options: String => Seq[(String, String)] = (journeyId: String) =>
    Seq(
      s"success~$journeyId"              -> "Success",
      s"preconditionFailed~$journeyId"   -> "Precondition Failed",
      s"lockedOut~$journeyId"            -> "Locked Out",
      s"insufficientEvidence~$journeyId" -> "Insufficient Evidence",
      s"failedMatching~$journeyId"       -> "Failed Matching",
      s"technicalIssue~$journeyId"       -> "Technical Issue",
      s"userAborted~$journeyId"          -> "User Aborted",
      s"timedOut~$journeyId"             -> "Timed Out",
      s"failedIV~$journeyId"             -> "Failed IV"
  )

  def showUpliftPage(
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector.getUser(credentials.providerId).map { currentUser =>
          val nino: String = currentUser.nino.map(_.value).getOrElse("")
          Ok(
            html
              .iv_uplift(
                UpliftRequestForm.fill(UpliftRequest(nino, "")),
                options(journeyIdValue),
                routes.IdentityVerificationController
                  .uplift(journeyIdValue, confidenceLevel, completionURL, failureURL, origin)
              ))
        }
      }
    }

  def uplift(
    journeyId: String,
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    html
                      .iv_uplift(
                        formWithErrors,
                        options(journeyId),
                        routes.IdentityVerificationController
                          .uplift(journeyId, confidenceLevel, completionURL, failureURL, origin)))),
              upliftRequest => {
                val journeyIdMatches = upliftRequest.option.contains(journeyId)
                val isSuccessful = upliftRequest.option.contains("success")
                if (journeyIdMatches && isSuccessful) {
                  for {
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(200))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield redirectWithJourneyId(completionURL.url, journeyId, upliftRequest.option)
                } else {
                  Future.successful(redirectWithJourneyId(failureURL.url, journeyId, upliftRequest.option)
                    .withHeaders("X-Result-Location" -> s"/mdtp/journey/journeyId/${upliftRequest.option}"))
                }
              }
            )
        }
    }

  private def redirectWithJourneyId(targetUrl: String, journeyId: String, option: String): Result = {
    val cleanUrl = URLDecoder.decode(targetUrl, "UTF-8")
    val tokenAppendChar = if (cleanUrl.contains("?")) "&" else "?"
    Redirect(s"$cleanUrl${tokenAppendChar}journeyId=$journeyId")
      .withHeaders("X-Result-Location" -> s"/mdtp/journey/journeyId/$option")
  }

  def getIvResult(journeyIdAndReason: String): Action[AnyContent] = Action.async { implicit request =>
    if (journeyIdAndReason.nonEmpty && journeyIdAndReason.contains("~")) {
      val split = journeyIdAndReason.split("~")
      val reason = split.head
      val journeyId = split.last
      Future.successful(Ok(Json.obj("token" -> journeyId, "reason" -> reason)))
    } else Future.successful(NotFound)
  }

  def showUpliftPageInternal(
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector.getUser(credentials.providerId).map { currentUser =>
          val nino: String = currentUser.nino.map(_.value).getOrElse("")
          Ok(
            html
              .iv_uplift(
                UpliftRequestForm.fill(UpliftRequest(nino, "")),
                options(journeyIdValue),
                routes.IdentityVerificationController
                  .uplift(journeyIdValue, confidenceLevel, completionURL, failureURL, origin)
              ))
        }
      }
    }

  def upliftInternal(
    journeyId: String,
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    html
                      .iv_uplift(
                        formWithErrors,
                        options(journeyId),
                        routes.IdentityVerificationController
                          .uplift(journeyId, confidenceLevel, completionURL, failureURL, origin)))),
              upliftRequest => {
                val journeyIdMatches = upliftRequest.option.contains(journeyId)
                val isSuccessful = upliftRequest.option.contains("success")
                if (journeyIdMatches && isSuccessful) {
                  for {
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(200))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield redirectWithJourneyId(completionURL.url, journeyId, upliftRequest.option)
                } else {
                  Future.successful(redirectWithJourneyId(failureURL.url, journeyId, upliftRequest.option))
                }
              }
            )
        }
    }
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
    )((nino, option) => UpliftRequest(nino.replace(" ", ""), option))(UpliftRequest.unapply))
}
