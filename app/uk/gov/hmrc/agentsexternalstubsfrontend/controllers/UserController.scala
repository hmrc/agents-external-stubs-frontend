package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Forms.{default, ignored, mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.{AuthConnector, NoActiveSession}
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

  def showUserPage(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentials) { credentials =>
          agentsExternalStubsConnector
            .getUser(credentials.providerId)
            .map(user =>
              Ok(html.show_user(user, routes.UserController.showEditUserPage(continue), continue, user.userId)))
        }
        .recover {
          case _: NoActiveSession =>
            Redirect(routes.SignInController.showSignInPage(continue, "agents-external-stubs", None))
        }
    }

  def showEditUserPage(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentials) { credentials =>
          agentsExternalStubsConnector
            .getUser(credentials.providerId)
            .map(
              user =>
                Ok(
                  html.edit_user(
                    UserForm.fill(user),
                    routes.UserController.updateUser(continue),
                    routes.UserController.showUserPage(continue),
                    user.userId,
                    continue.isDefined)))
        }
        .recover {
          case _: NoActiveSession =>
            Redirect(routes.SignInController.showSignInPage(continue, "agents-external-stubs", None))
        }
    }

  def updateUser(continue: Option[ContinueUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentials) { credentials =>
          UserForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    html.edit_user(
                      formWithErrors,
                      routes.UserController.updateUser(continue),
                      routes.UserController.showUserPage(continue),
                      credentials.providerId,
                      continue.isDefined))),
              user =>
                agentsExternalStubsConnector
                  .updateUser(withTidyEnrolments(user.copy(userId = credentials.providerId)))
                  .map(_ =>
                    continue.fold(Redirect(routes.UserController.showUserPage(continue)))(continueUrl =>
                      Redirect(continueUrl.url)))
            )
        }
        .recover {
          case _: NoActiveSession =>
            Redirect(routes.SignInController.showSignInPage(continue, "agents-external-stubs", None))
        }
    }

  private def withTidyEnrolments(user: User): User = user

}

object UserController {

  val none = "none"

  val identifierMapping: Mapping[Identifier] = mapping(
    "key"   -> nonEmptyText,
    "value" -> nonEmptyText
  )(Identifier.apply)(Identifier.unapply)

  val enrolmentMapping: Mapping[Option[Enrolment]] = optional(
    mapping(
      "key"         -> nonEmptyText,
      "identifiers" -> optional(seq(identifierMapping))
    )(Enrolment.apply)(Enrolment.unapply))

  def onlyAgentCanHaveDelegatedEnrolments: Constraint[User] =
    Constraint(
      user =>
        if (user.delegatedEnrolments.nonEmpty && !user.affinityGroup.contains("Agent"))
          Invalid("user.form.error.onlyAgentCanHaveDelegatedEnrolments")
        else Valid)

  def onlyIndividualsCanHaveNino: Constraint[User] =
    Constraint(
      user =>
        if (user.nino.nonEmpty && !user.affinityGroup.contains("Individual"))
          Invalid("user.form.error.onlyIndividualsCanHaveNino")
        else Valid)

  def confidenceLevelMustBeDefinedWithNino: Constraint[User] =
    Constraint(
      user =>
        if (user.nino.nonEmpty != user.confidenceLevel.nonEmpty)
          Invalid("user.form.error.confidenceLevelMustBeDefinedForNino")
        else Valid)

  val UserForm: Form[User] = Form[User](
    mapping(
      "userId"             -> ignored("ignored"),
      "groupId"            -> optional(text),
      "affinityGroup"      -> optional(text).transform(fromNone(none), toNone(none)),
      "confidenceLevel"    -> optional(number).transform(fromNone(0), toNone(0)),
      "credentialStrength" -> optional(text).transform(fromNone(none), toNone(none)),
      "credentialRole"     -> optional(text).transform(fromNone(none), toNone(none)),
      "nino" -> optional(text)
        .verifying("user.form.nino.error", _.forall(Nino.isValid))
        .transform[Option[Nino]](_.map(Nino.apply), _.map(_.toString)),
      "principalEnrolments" -> seq(enrolmentMapping)
        .transform[Seq[Enrolment]](_.collect { case Some(x) => x }, _.map(Option.apply)),
      "delegatedEnrolments" -> seq(enrolmentMapping)
        .transform[Seq[Enrolment]](_.collect { case Some(x) => x }, _.map(Option.apply))
    )(User.apply)(User.unapply)
      .verifying(onlyAgentCanHaveDelegatedEnrolments, onlyIndividualsCanHaveNino, confidenceLevelMustBeDefinedWithNino))

  def fromNone[T](none: T): Option[T] => Option[T] = {
    case Some(`none`) => None
    case s            => s
  }

  def toNone[T](none: T): Option[T] => Option[T] = {
    case None => Some(none)
    case s    => s
  }
}
