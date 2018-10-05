package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Forms.{boolean, ignored, mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Address, Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubsfrontend.services.{Features, ServicesDefinitionsService}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{NotFoundException, SessionKeys}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class UserController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val servicesDefinitionsService: ServicesDefinitionsService,
  val features: Features
)(implicit val configuration: Configuration)
    extends FrontendController with AuthActions with I18nSupport with WithPageContext {

  import UserController._

  val start: Action[AnyContent] = showUserPage(None, None)

  def showUserPage(continue: Option[ContinueUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector
            .getUser(userId.getOrElse(credentials.providerId))
            .map(user =>
              Ok(html.show_user(
                user,
                request.session.get(SessionKeys.authToken),
                request.session.get(SessionKeys.sessionId),
                routes.UserController.showEditUserPage(continue, userId),
                continue,
                user.userId,
                pageContext(credentials)
              )))
        }
    }

  def showCreateUserPage(continue: Option[ContinueUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          userId match {
            case Some(uid) =>
              agentsExternalStubsConnector
                .getUser(uid)
                .map(
                  user =>
                    if (user.affinityGroup.isDefined)
                      Redirect(routes.UserController.showEditUserPage(continue, userId))
                    else
                      Ok(html.create_user(
                        UserForm.fill(user),
                        routes.UserController
                          .updateUser(
                            Some(ContinueUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                            userId,
                            create = true),
                        routes.UserController.showEditUserPage(continue, userId),
                        routes.UserController.showUserPage(continue, userId),
                        user.userId,
                        continue.isDefined,
                        servicesDefinitionsService.servicesDefinitions.options,
                        pageContext(credentials)
                      )))
                .recover {
                  case e: NotFoundException =>
                    userId match {
                      case Some(id) =>
                        Ok(
                          html.create_user(
                            UserForm.fill(User(id)),
                            routes.UserController
                              .updateUser(
                                Some(ContinueUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                                userId,
                                create = true),
                            routes.UserController.showEditUserPage(continue, userId),
                            routes.UserController.showUserPage(continue, userId),
                            id,
                            continue.isDefined,
                            servicesDefinitionsService.servicesDefinitions.options,
                            pageContext(credentials)
                          ))
                      case None => NotFound(e.getMessage)
                    }
                }
            case None =>
              Future.successful(
                Ok(html.create_user(
                  UserForm.fill(User(credentials.providerId)),
                  routes.UserController
                    .updateUser(Some(ContinueUrl(routes.UserController.showEditUserPage(continue).url)), create = true),
                  routes.UserController.showEditUserPage(continue),
                  routes.UserController.showUserPage(continue),
                  credentials.providerId,
                  continue.isDefined,
                  servicesDefinitionsService.servicesDefinitions.options,
                  pageContext(credentials)
                )))
          }
        }
    }

  def showEditUserPage(continue: Option[ContinueUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector
            .getUser(userId.getOrElse(credentials.providerId))
            .map(user =>
              Ok(html.edit_user(
                UserForm.fill(user),
                routes.UserController.updateUser(continue, userId),
                routes.UserController.showUserPage(continue, userId),
                user.userId,
                continue.isDefined,
                pageContext(credentials)
              )))
        }
    }

  def updateUser(continue: Option[ContinueUrl], userId: Option[String], create: Boolean): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UserForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                if (create)
                  Future.successful(Ok(html.create_user(
                    UserForm.fill(User(userId.get)),
                    routes.UserController
                      .updateUser(
                        Some(ContinueUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                        userId,
                        create = true),
                    routes.UserController.showEditUserPage(continue, userId),
                    routes.UserController.showUserPage(continue, userId),
                    userId.get,
                    continue.isDefined,
                    servicesDefinitionsService.servicesDefinitions.options,
                    pageContext(credentials)
                  )))
                else
                  Future.successful(Ok(html.edit_user(
                    formWithErrors,
                    routes.UserController.updateUser(continue, userId),
                    routes.UserController.showUserPage(continue, userId),
                    userId = userId.getOrElse(credentials.providerId),
                    continue.isDefined,
                    pageContext(credentials)
                  ))),
              user =>
                (if (create && userId.isDefined)
                   agentsExternalStubsConnector.createUser(user.copy(userId = userId.get))
                 else
                   agentsExternalStubsConnector
                     .updateUser(user.copy(userId = userId.getOrElse(credentials.providerId))))
                  .map(_ =>
                    continue.fold(Redirect(routes.UserController.showUserPage(continue, userId)))(continueUrl =>
                      Redirect(continueUrl.url)))
                  .recover {
                    case e: Exception =>
                      Ok(html.edit_user(
                        UserForm.fill(user).withGlobalError(e.getMessage),
                        routes.UserController.updateUser(continue, userId),
                        routes.UserController.showUserPage(continue, userId),
                        userId = userId.getOrElse(credentials.providerId),
                        continue.isDefined,
                        pageContext(credentials)
                      ))
                }
            )
        }
    }

  def removeUser(continue: Option[ContinueUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          val id = userId.getOrElse(credentials.providerId)
          (if (id == credentials.providerId) Future.successful(())
           else agentsExternalStubsConnector.removeUser(id))
            .map(_ => Redirect(routes.UserController.showAllUsersPage()))
            .recover {
              case NonFatal(e) =>
                Ok(html.error_template(messagesApi("error.title"), s"Error while removing $id", e.getMessage))
            }
        }
    }

  val showAllUsersPage: Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector.getUsers
            .map(
              users =>
                Ok(
                  html.show_all_users(
                    users,
                    request.session.get(SessionKeys.authToken),
                    routes.UserController.showUserPage(None),
                    pageContext(credentials)
                  )))
        }
    }
}

object UserController {

  val none = "none"

  val identifierMapping: Mapping[Identifier] = mapping(
    "key"   -> text,
    "value" -> text
  )(Identifier.apply)(Identifier.unapply)

  val enrolmentMapping: Mapping[Option[Enrolment]] = optional(
    mapping(
      "key"         -> nonEmptyText,
      "identifiers" -> optional(seq(identifierMapping))
    )(Enrolment.apply)(Enrolment.unapply))

  val addressMapping: Mapping[Address] = mapping(
    "line1"       -> optional(nonEmptyText),
    "line2"       -> optional(nonEmptyText),
    "line3"       -> optional(nonEmptyText),
    "line4"       -> optional(nonEmptyText),
    "postcode"    -> optional(nonEmptyText),
    "countryCode" -> optional(nonEmptyText)
  )(Address.apply)(Address.unapply)

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
      "principalEnrolments" -> optional(seq(enrolmentMapping))
        .transform[Option[Seq[Enrolment]]](_.map(_.collect { case Some(x) => x }), _.map(_.map(Option.apply))),
      "delegatedEnrolments" -> optional(seq(enrolmentMapping))
        .transform[Option[Seq[Enrolment]]](_.map(_.collect { case Some(x) => x }), _.map(_.map(Option.apply))),
      "name"              -> optional(nonEmptyText),
      "dateOfBirth"       -> optional(DateFieldHelper.dateFieldsMapping(DateFieldHelper.validDobDateFormat)),
      "agentCode"         -> optional(nonEmptyText),
      "agentFriendlyName" -> optional(nonEmptyText),
      "isNonCompliant"    -> optional(boolean),
      "complianceIssues"  -> ignored[Option[Seq[String]]](None),
      "isPermanent"       -> optional(boolean),
      "recordIds"         -> ignored[Option[Seq[String]]](None),
      "address"           -> optional(addressMapping)
    )(User.apply)(User.unapply))

  def fromNone[T](none: T): Option[T] => Option[T] = {
    case Some(`none`) => None
    case s            => s
  }

  def toNone[T](none: T): Option[T] => Option[T] = {
    case None => Some(none)
    case s    => s
  }

}
