package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Forms.{mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, SpecialCase}
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SpecialCasesController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val features: Features
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport with WithPageContext {

  import SpecialCasesController._

  def showAllSpecialCasesPage(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        agentsExternalStubsConnector.getAllSpecialCases
          .map(
            specialCases =>
              Ok(html.show_all_special_cases(
                specialCases,
                caseId,
                id => routes.SpecialCasesController.showAllSpecialCasesPage(Some(id)),
                id => routes.SpecialCasesController.showEditSpecialCasePage(Some(id)),
                routes.SpecialCasesController.deleteSpecialCase,
                pageContext(credentials)
              )))
      }
  }

  def showEditSpecialCasePage(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        caseId match {
          case Some(id) =>
            agentsExternalStubsConnector
              .getSpecialCase(id)
              .map {
                case Some(specialCase) =>
                  Ok(
                    html.edit_special_case(
                      SpecialCaseForm.fill(specialCase),
                      caseId,
                      routes.SpecialCasesController.upsertSpecialCase(caseId),
                      routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                      routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                      pageContext(credentials)
                    ))
                case None =>
                  Ok(
                    html.edit_special_case(
                      SpecialCaseForm.withGlobalError(s"Special Case with id=$id has not been found."),
                      caseId,
                      routes.SpecialCasesController.upsertSpecialCase(None),
                      routes.SpecialCasesController.showEditSpecialCasePage(None),
                      routes.SpecialCasesController.showAllSpecialCasesPage(None),
                      pageContext(credentials)
                    ))
              }
          case None =>
            Future.successful(
              Ok(html.edit_special_case(
                SpecialCaseForm,
                None,
                routes.SpecialCasesController.upsertSpecialCase(caseId),
                routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                pageContext(credentials)
              )))
        }
      }
  }

  def upsertSpecialCase(caseId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
        SpecialCaseForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(Ok(html.edit_special_case(
                formWithErrors,
                caseId,
                routes.SpecialCasesController.upsertSpecialCase(caseId),
                routes.SpecialCasesController.showEditSpecialCasePage(caseId),
                routes.SpecialCasesController.showAllSpecialCasesPage(caseId),
                pageContext(credentials)
              ))),
            specialCase =>
              (caseId match {
                case Some(_) =>
                  agentsExternalStubsConnector.updateSpecialCase(specialCase.copy(id = caseId))
                case None =>
                  agentsExternalStubsConnector.createSpecialCase(specialCase)
              }).map(id => Redirect(routes.SpecialCasesController.showAllSpecialCasesPage(Some(id))))
          )
      }
  }

  def deleteSpecialCase(caseId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised()
      .retrieve(Retrievals.credentialsWithPlanetId) { _ =>
        agentsExternalStubsConnector
          .deleteSpecialCase(caseId)
          .map(_ => Redirect(routes.SpecialCasesController.showAllSpecialCasesPage(None)))
      }
  }

}

object SpecialCasesController {

  val requestMatchMapping: Mapping[SpecialCase.RequestMatch] = mapping(
    "path" -> nonEmptyText
      .transform[String](path => if (!path.startsWith("/")) "/" + path else path, identity)
      .verifying(path => path != "/")
      .verifying(path => !(path.startsWith("/agents-external-stubs") || path.startsWith("/ping"))),
    "method"      -> nonEmptyText,
    "body"        -> optional(nonEmptyText),
    "contentType" -> optional(nonEmptyText)
  )(SpecialCase.RequestMatch.apply)(SpecialCase.RequestMatch.unapply)

  val headerMapping: Mapping[SpecialCase.Header] = mapping(
    "name"  -> nonEmptyText,
    "value" -> nonEmptyText
  )(SpecialCase.Header.apply)(SpecialCase.Header.unapply)

  val responseMapping = mapping(
    "status" -> number(200, 599),
    "body"   -> optional(nonEmptyText),
    "headers" -> optional(seq(optional(headerMapping)))
      .transform[Option[Seq[SpecialCase.Header]]](_.map(_.collect { case Some(x) => x }), _.map(_.map(Option.apply)))
  )(SpecialCase.Response.apply)(SpecialCase.Response.unapply)

  val SpecialCaseForm: Form[SpecialCase] = Form[SpecialCase](
    mapping(
      "requestMatch" -> requestMatchMapping,
      "response"     -> responseMapping,
      "id"           -> optional(text)
    )(SpecialCase.apply)(SpecialCase.unapply))

}
