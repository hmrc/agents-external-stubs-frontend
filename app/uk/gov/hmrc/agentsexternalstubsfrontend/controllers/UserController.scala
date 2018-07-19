package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Forms.{mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class UserController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport {

  import UserController._

  def showEditUserPage(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentials) { credentials =>
        agentsExternalStubsConnector
          .getUser(credentials.providerId)
          .map(user => Ok(html.edit_user(UserForm.fill(user), routes.UserController.updateUser(continue))))
      }
    }

  def updateUser(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(Retrievals.credentials) { credentials =>
        UserForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(Ok(html.edit_user(formWithErrors, routes.UserController.updateUser(continue)))),
            user =>
              agentsExternalStubsConnector
                .updateUser(user.copy(userId = credentials.providerId))
                .map(_ =>
                  continue.fold(Ok(s"User ${credentials.providerId} has been updated"))(continueUrl =>
                    Redirect(continueUrl.url, Map.empty, 303)))
          )
      }
    }

}

object UserController {

  val identifierMapping: Mapping[Identifier] = mapping(
    "key"   -> nonEmptyText,
    "value" -> nonEmptyText
  )(Identifier.apply)(Identifier.unapply)

  val enrolmentMapping: Mapping[Enrolment] = mapping(
    "key"         -> nonEmptyText,
    "identifiers" -> optional(seq(identifierMapping))
  )(Enrolment.apply)(Enrolment.unapply)

  val UserForm: Form[User] = Form[User](
    mapping(
      "userId"              -> text,
      "groupId"             -> optional(text),
      "affinityGroup"       -> optional(text),
      "confidenceLevel"     -> optional(number).transform[Int](_.getOrElse(50), n => Some(n)),
      "credentialStrength"  -> optional(text),
      "credentialRole"      -> optional(text),
      "nino"                -> optional(text).transform[Option[Nino]](_.map(Nino.apply), _.map(_.toString())),
      "principalEnrolments" -> seq(enrolmentMapping),
      "delegatedEnrolments" -> seq(enrolmentMapping)
    )(User.apply)(User.unapply))

}
