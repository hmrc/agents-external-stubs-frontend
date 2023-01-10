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

import com.google.inject.Provider
import play.api.Configuration
import play.api.data.Forms.{boolean, ignored, mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.agentsexternalstubsfrontend.services.{Features, ServicesDefinitionsService}
import uk.gov.hmrc.agentsexternalstubsfrontend.views.html._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{NotFoundException, SessionKeys}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class UserController @Inject() (
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentsExternalStubsConnector: AgentsExternalStubsConnector,
  val servicesDefinitionsService: ServicesDefinitionsService,
  showUserView: show_user,
  createUserView: create_user,
  editUserView: edit_user,
  errorTemplateView: error_template,
  showAllUsersView: show_all_users,
  granPermsUserGenView: gran_perms_user_gen,
  val features: Features,
  ecp: Provider[ExecutionContext]
)(implicit val configuration: Configuration, cc: MessagesControllerComponents)
    extends FrontendController(cc) with AuthorisedFunctions with I18nSupport with WithPageContext {

  implicit val ec: ExecutionContext = ecp.get

  import UserController._

  val start: Action[AnyContent] = showUserPage(None, None)

  def showUserPage(continue: Option[RedirectUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          for {
            user <- agentsExternalStubsConnector.getUser(userId.getOrElse(credentials.providerId))
            maybeGroup <- user.groupId match {
                            case None          => Future.successful(None)
                            case Some(groupId) => agentsExternalStubsConnector.getGroup(groupId).map(Some(_))
                          }
          } yield Ok(
            showUserView(
              user,
              maybeGroup,
              request.session.get(SessionKeys.authToken),
              request.session.get(SessionKeys.sessionId),
              routes.UserController.showEditUserPage(continue, userId),
              continue,
              user.userId,
              maybeGroup
                .map(group =>
                  servicesDefinitionsService.servicesDefinitions
                    .servicesFor(group.affinityGroup)
                    .collect {
                      case s if s.flags.multipleEnrolment   => s.name
                      case s if !user.isEnrolledFor(s.name) => s.name
                    }
                )
                .getOrElse(Seq.empty),
              pageContext(credentials)
            )
          )
        }
    }

  def showCreateUserPage(continue: Option[RedirectUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          userId match {
            case Some(uid) =>
              agentsExternalStubsConnector
                .getUser(uid)
                .map(user =>
                  Ok(
                    createUserView(
                      InitialUserCreationData.form,
                      routes.UserController
                        .submitCreateUserPage(
                          Some(RedirectUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                          uid
                        ),
                      routes.UserController.showUserPage(continue, userId),
                      user.userId,
                      continue.isDefined,
                      servicesDefinitionsService.servicesDefinitions.options,
                      pageContext(credentials)
                    )
                  )
                )
                .recover { case e: NotFoundException =>
                  userId match {
                    case Some(id) =>
                      Ok(
                        createUserView(
                          InitialUserCreationData.form,
                          routes.UserController
                            .submitCreateUserPage(
                              Some(RedirectUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                              uid
                            ),
                          routes.UserController.showUserPage(continue, userId),
                          id,
                          continue.isDefined,
                          servicesDefinitionsService.servicesDefinitions.options,
                          pageContext(credentials)
                        )
                      )
                    case None => NotFound(e.getMessage)
                  }
                }
            case None =>
              Future.successful(
                Ok(
                  createUserView(
                    InitialUserCreationData.form,
                    routes.UserController
                      .submitCreateUserPage(
                        Some(RedirectUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                        credentials.providerId
                      ),
                    routes.UserController.showUserPage(continue),
                    credentials.providerId,
                    continue.isDefined,
                    servicesDefinitionsService.servicesDefinitions.options,
                    pageContext(credentials)
                  )
                )
              )
          }
        }
    }

  def submitCreateUserPage(continue: Option[RedirectUrl], userId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          InitialUserCreationData.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    createUserView(
                      formWithErrors,
                      routes.UserController
                        .submitCreateUserPage(
                          continue,
                          userId
                        ),
                      routes.UserController.showUserPage(continue, Some(userId)),
                      userId,
                      continue.isDefined,
                      servicesDefinitionsService.servicesDefinitions.options,
                      pageContext(credentials)
                    )
                  )
                ),
              initialUserCreationData =>
                for {
                  // delete the old temp user (if any) before recreating one with the desired affinity group and enrolment
                  _ <- agentsExternalStubsConnector.removeUser(userId).recover { case _ => () }
                  _ <- agentsExternalStubsConnector.createUser(
                         user = User(
                           userId = userId,
                           assignedPrincipalEnrolments = initialUserCreationData.principalEnrolmentService
                             .filter(e => e.nonEmpty && e != "none")
                             .map(EnrolmentKey(_, identifiers = Seq.empty))
                             .toSeq
                         ),
                         affinityGroup = initialUserCreationData.affinityGroup.filter(e => e.nonEmpty && e != "none")
                       )
                } yield continue.fold(Redirect(routes.UserController.showUserPage(continue, Some(userId))))(
                  continueUrl => Redirect(continueUrl.unsafeValue)
                )
            )
        }
  }

  def showEditUserPage(continue: Option[RedirectUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          for {
            user <- agentsExternalStubsConnector.getUser(userId.getOrElse(credentials.providerId))
            affinityGroup <- user.groupId match {
                               case Some(gid) =>
                                 agentsExternalStubsConnector.getGroup(gid).map(group => Some(group.affinityGroup))
                               case None => Future.successful(None)
                             }
          } yield Ok(
            editUserView(
              UserForm.fill(user),
              affinityGroup,
              routes.UserController.updateUser(continue, userId),
              routes.UserController.showUserPage(continue, userId),
              user.userId,
              continue.isDefined,
              pageContext(credentials)
            )
          )
        }
    }

  def updateUser(
    continue: Option[RedirectUrl],
    userId: Option[String],
    create: Boolean,
    affinityGroup: Option[String]
  ): Action[AnyContent] =
    if (create)
      updateNewUser(continue, userId, affinityGroup)
    else
      updateExistingUser(continue, userId, affinityGroup)

  def updateExistingUser(
    continue: Option[RedirectUrl],
    userId: Option[String],
    affinityGroup: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UserForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    editUserView(
                      formWithErrors,
                      affinityGroup,
                      routes.UserController.updateUser(continue, userId),
                      routes.UserController.showUserPage(continue, userId),
                      userId = userId.getOrElse(credentials.providerId),
                      continue.isDefined,
                      pageContext(credentials)
                    )
                  )
                ),
              user =>
                (agentsExternalStubsConnector
                  .updateUser(user.copy(userId = userId.getOrElse(credentials.providerId))))
                  .map(_ =>
                    continue.fold(Redirect(routes.UserController.showUserPage(continue, userId)))(continueUrl =>
                      Redirect(continueUrl.unsafeValue)
                    )
                  )
                  .recover { case e: Exception =>
                    Ok(
                      editUserView(
                        UserForm.fill(user).withGlobalError(e.getMessage),
                        affinityGroup,
                        routes.UserController.updateUser(continue, userId),
                        routes.UserController.showUserPage(continue, userId),
                        userId = userId.getOrElse(credentials.providerId),
                        continue.isDefined,
                        pageContext(credentials)
                      )
                    )
                  }
            )
        }
    }

  def updateNewUser(
    continue: Option[RedirectUrl],
    userId: Option[String],
    affinityGroup: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          UserForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  Ok(
                    createUserView(
                      InitialUserCreationData.form,
                      routes.UserController
                        .submitCreateUserPage(
                          Some(RedirectUrl(routes.UserController.showEditUserPage(continue, userId).url)),
                          userId.get
                        ),
                      routes.UserController.showUserPage(continue, userId),
                      userId.get,
                      continue.isDefined,
                      servicesDefinitionsService.servicesDefinitions.options,
                      pageContext(credentials)
                    )
                  )
                ),
              user =>
                (if (userId.isDefined)
                   agentsExternalStubsConnector.createUser(
                     user.copy(
                       userId = userId.get,
                       confidenceLevel =
                         if (affinityGroup.contains(User.Individual)) Some(250) else user.confidenceLevel,
                       credentialStrength =
                         if (affinityGroup.contains(User.Individual)) Some("strong")
                         else user.credentialStrength
                     ),
                     affinityGroup
                   )
                 else
                   agentsExternalStubsConnector
                     .updateUser(
                       user.copy(
                         userId = credentials.providerId,
                         confidenceLevel =
                           if (affinityGroup.contains(User.Individual)) Some(250) else user.confidenceLevel,
                         credentialStrength =
                           if (affinityGroup.contains(User.Individual)) Some("strong")
                           else user.credentialStrength
                       )
                     ))
                  .map(_ =>
                    continue.fold(Redirect(routes.UserController.showUserPage(continue, userId)))(continueUrl =>
                      Redirect(continueUrl.unsafeValue)
                    )
                  )
                  .recover { case e: Exception =>
                    Ok(
                      editUserView(
                        UserForm.fill(user).withGlobalError(e.getMessage),
                        affinityGroup,
                        routes.UserController.updateUser(continue, userId),
                        routes.UserController.showUserPage(continue, userId),
                        userId = userId.getOrElse(credentials.providerId),
                        continue.isDefined,
                        pageContext(credentials)
                      )
                    )
                  }
            )
        }
    }

  def removeUser(continue: Option[RedirectUrl], userId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          val id = userId.getOrElse(credentials.providerId)
          (if (id == credentials.providerId) Future.successful(())
           else agentsExternalStubsConnector.removeUser(id))
            .map(_ => Redirect(routes.UserController.showAllUsersPage))
            .recover { case NonFatal(e) =>
              Ok(errorTemplateView("error.title", s"Error while removing $id", e.getMessage))
            }
        }
    }

  def amendUser(
    continue: Option[RedirectUrl],
    userId: Option[String],
    assignedPrincipalEnrolment: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          val id = userId.getOrElse(credentials.providerId)
          agentsExternalStubsConnector
            .getUser(id)
            .flatMap { user =>
              val maybeUpdate =
                assignedPrincipalEnrolment.map { newEnrolment =>
                  val enrolmentKey =
                    if (newEnrolment.split("~").length == 1)
                      // if only service key is provided (no identifiers), do not attempt to parse as a full enrolment key
                      EnrolmentKey(newEnrolment, identifiers = Seq.empty)
                    else
                      // if it is a full enrolment key, parse normally
                      Enrolment(newEnrolment).toEnrolmentKey.get
                  user.updateAssignedPrincipalEnrolments(_ :+ enrolmentKey)
                }
              (maybeUpdate match {
                case Some(update) => agentsExternalStubsConnector.updateUser(update)
                case None         => Future.successful(())
              }).map(_ => Redirect(routes.UserController.showUserPage(continue, userId)))
            }
        }
    }

  def showGranPermsCreateUsers: Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          Future.successful(Ok(granPermsUserGenView(GranPermsGenRequestForm.form)))
        }
    }

  def submitGranPermsCreateUsers: Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          GranPermsGenRequestForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(formWithErrors.errors.toString)),
              genRequest =>
                agentsExternalStubsConnector
                  .massCreateAssistantsAndUsers(genRequest)
                  .map(genResponse => Ok(Json.toJson(genResponse)))
                  .recover { case e =>
                    InternalServerError(e.getMessage)
                  }
            )
        }
    }

  val showAllUsersPage: Action[AnyContent] =
    Action.async { implicit request =>
      authorised()
        .retrieve(Retrievals.credentialsWithPlanetId) { credentials =>
          agentsExternalStubsConnector.getUsers
            .map(users =>
              Ok(
                showAllUsersView(
                  users,
                  request.session.get(SessionKeys.authToken),
                  routes.UserController.showUserPage(None),
                  pageContext(credentials)
                )
              )
            )
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
    )(Enrolment.apply)(Enrolment.unapply)
  )

  val enrolmentKeyMapping: Mapping[Option[EnrolmentKey]] = optional(
    mapping(
      "key"         -> nonEmptyText,
      "identifiers" -> seq(identifierMapping)
    )(EnrolmentKey.apply)(EnrolmentKey.unapply)
  )
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
      "confidenceLevel"    -> optional(number).transform(fromNone(0), toNone(0)),
      "credentialStrength" -> optional(text).transform(fromNone(none), toNone(none)),
      "credentialRole"     -> optional(text).transform(fromNone(none), toNone(none)),
      "nino" -> optional(text)
        .verifying("user.form.nino.error", _.forall(Nino.isValid))
        .transform[Option[Nino]](_.map(Nino.apply), _.map(_.toString)),
      "assignedPrincipalEnrolments" -> seq(enrolmentKeyMapping)
        .transform[Seq[EnrolmentKey]](_.collect { case Some(ek) => ek }, _.map(Some(_))),
      "assignedDelegatedEnrolments" -> seq(enrolmentKeyMapping)
        .transform[Seq[EnrolmentKey]](_.collect { case Some(ek) => ek }, _.map(Some(_))),
      "name"             -> optional(nonEmptyText),
      "dateOfBirth"      -> optional(DateFieldHelper.dateFieldsMapping(DateFieldHelper.validDobDateFormat)),
      "isNonCompliant"   -> optional(boolean),
      "complianceIssues" -> ignored[Option[Seq[String]]](None),
      "isPermanent"      -> optional(boolean),
      "recordIds"        -> ignored[Option[Seq[String]]](None),
      "address"          -> optional(addressMapping),
      "strideRoles" -> optional(nonEmptyText)
        .transform[Seq[String]](_.map(_.split(",").toSeq).getOrElse(Seq.empty), s => Some(s.mkString(",")))
    )(User.apply)(User.unapply)
  )

  def fromNone[T](none: T): Option[T] => Option[T] = {
    case Some(`none`) => None
    case s            => s
  }

  def toNone[T](none: T): Option[T] => Option[T] = {
    case None => Some(none)
    case s    => s
  }

  case class InitialUserCreationData(
    affinityGroup: Option[String],
    principalEnrolmentService: Option[String]
  )

  object InitialUserCreationData {
    val form: Form[InitialUserCreationData] =
      Form[InitialUserCreationData](
        mapping(
          "affinityGroup"             -> optional(text),
          "principalEnrolmentService" -> optional(text)
        )(InitialUserCreationData.apply)(InitialUserCreationData.unapply)
      )
  }

}
