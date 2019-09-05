package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import java.net.URLDecoder

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OFormat, Writes}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.iv_models.JourneyType.{UpliftNino, UpliftNoNino}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.iv_models.ServiceContract
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

  def showUpliftPage(
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        for {
          currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
          journeyId <- {
            val journeyType = if (currentUser.nino.isDefined) UpliftNino else UpliftNoNino
            val serviceContract = ServiceContract(origin, completionURL.url, failureURL.url, confidenceLevel)
            agentsExternalStubsConnector.createJourneyId(serviceContract, journeyType)
          }
        } yield {
          val nino = currentUser.nino.map(_.value).getOrElse("")
          Ok(
            html
              .iv_uplift(
                UpliftRequestForm.fill(UpliftRequest(nino, "")),
                routes.IdentityVerificationController.uplift(journeyId)))
        }
      }
    }

  def uplift(journeyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(Ok(html
                  .iv_uplift(formWithErrors, routes.IdentityVerificationController.uplift(journeyId)))),
              upliftRequest => {
                if (upliftRequest.option == "success") {
                  for {
                    journey     <- agentsExternalStubsConnector.getJourneyId(journeyId)
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(200))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield redirectWithJourneyId(journey.completionURL, journeyId)
                } else {
                  for {
                    journey <- agentsExternalStubsConnector.getJourneyId(journeyId)
                  } yield redirectWithJourneyId(journey.failureURL, journeyId)
                }
              }
            )
        }
    }

  private def redirectWithJourneyId(targetUrl: String, journeyId: String): Result = {
    val cleanUrl = URLDecoder.decode(targetUrl, "UTF-8")
    val tokenAppendChar = if (cleanUrl.contains("?")) "&" else "?"
    Redirect(s"$cleanUrl${tokenAppendChar}journeyId=$journeyId")
  }

  def showUpliftPageInternal(
    confidenceLevel: Int,
    completionURL: ContinueUrl,
    failureURL: ContinueUrl,
    origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        for {
          currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
          journeyId <- {
            val journeyType = if (currentUser.nino.isDefined) UpliftNino else UpliftNoNino
            val serviceContract = ServiceContract(origin, completionURL.url, failureURL.url, confidenceLevel)
            agentsExternalStubsConnector.createJourneyId(serviceContract, journeyType)
          }
        } yield {
          val nino = currentUser.nino.map(_.value).getOrElse("")
          Ok(
            html
              .iv_uplift(
                UpliftRequestForm.fill(UpliftRequest(nino, "")),
                routes.IdentityVerificationController.uplift(journeyId)))
        }
      }
    }

  def upliftInternal(journeyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UpliftRequestForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(Ok(html
                  .iv_uplift(formWithErrors, routes.IdentityVerificationController.uplift(journeyId)))),
              upliftRequest => {
                if (upliftRequest.option == "success") {
                  for {
                    journey     <- agentsExternalStubsConnector.getJourneyId(journeyId)
                    currentUser <- agentsExternalStubsConnector.getUser(credentials.providerId)
                    modifiedUser = currentUser.copy(confidenceLevel = Some(200))
                    _ <- agentsExternalStubsConnector.updateUser(modifiedUser)
                  } yield redirectWithJourneyId(journey.completionURL, journeyId)
                } else {
                  for {
                    journey <- agentsExternalStubsConnector.getJourneyId(journeyId)
                  } yield redirectWithJourneyId(journey.failureURL, journeyId)
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
