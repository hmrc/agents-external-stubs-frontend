package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class RecordsController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  import RecordsController._

  def showAllRecordsPage(showId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      agentsExternalStubsConnector.getRecords
        .map(records => Ok(html.show_all_records(records, request.session.get("planetId").getOrElse(""), showId)))
    }
  }

  def deleteRecord(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      agentsExternalStubsConnector
        .deleteRecord(id)
        .map(_ => Redirect(routes.RecordsController.showAllRecordsPage()))
    }
  }

  def showEditRecordPage(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      agentsExternalStubsConnector
        .getRecord(id)
        .map(
          record =>
            Ok(
              html.edit_record(
                RecordForm.fill(record.-("id").-("_links")),
                routes.RecordsController.updateRecord(id),
                routes.RecordsController.showAllRecordsPage(Some(id)))))
    }
  }

  def updateRecord(id: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      RecordForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(
                html.edit_record(
                  formWithErrors,
                  routes.RecordsController.updateRecord(id),
                  routes.RecordsController.showAllRecordsPage(Some(id))))),
          record =>
            agentsExternalStubsConnector
              .updateRecord(id, record)
              .map(_ => Redirect(routes.RecordsController.showAllRecordsPage(Some(id))))
              .recover {
                case e =>
                  Ok(html.edit_record(
                    RecordForm.fill(record).withError("json", e.getMessage),
                    routes.RecordsController.updateRecord(id),
                    routes.RecordsController.showAllRecordsPage(Some(id))
                  ))
            }
        )
    }
  }

  def showAddRecordPage(`type`: String, seed: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      agentsExternalStubsConnector
        .generateRecord(`type`, seed)
        .map(record =>
          Ok(html.create_record(
            RecordForm.fill(record.-("id").-("_links")),
            routes.RecordsController.createRecord(`type`, seed),
            routes.RecordsController.showAllRecordsPage(None),
            routes.RecordsController.showAddRecordPage(`type`, shake(seed))
          )))
    }
  }

  def createRecord(`type`: String, seed: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      RecordForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(Ok(html.create_record(
              formWithErrors,
              routes.RecordsController.createRecord(`type`, seed),
              routes.RecordsController.showAllRecordsPage(None),
              routes.RecordsController.showAddRecordPage(`type`, shake(seed))
            ))),
          record =>
            agentsExternalStubsConnector
              .createRecord(`type`, record)
              .map(recordIdOpt => Redirect(routes.RecordsController.showAllRecordsPage(recordIdOpt)))
              .recover {
                case e =>
                  Ok(html.create_record(
                    RecordForm.fill(record).withError("json", e.getMessage),
                    routes.RecordsController.createRecord(`type`, seed),
                    routes.RecordsController.showAllRecordsPage(None),
                    routes.RecordsController.showAddRecordPage(`type`, shake(seed))
                  ))
            }
        )
    }
  }

  private def shake(seed: String): String = {
    val p = seed.charAt(0).toInt % seed.length
    val s = seed.drop(1) + seed.head
    s.take(p).reverse + s.drop(p)

  }

}

object RecordsController {

  val validJson: Constraint[String] = Constraint(
    json => try { Json.parse(json); Valid } catch { case NonFatal(e) => Invalid(e.getMessage) })

  val RecordForm: Form[JsObject] =
    Form[JsObject](
      mapping("json" -> text.verifying(validJson))(t => Json.parse(t).as[JsObject])(j => Some(Json.prettyPrint(j))))
}
