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

package uk.gov.hmrc.agentsexternalstubsfrontend.forms

import play.api.data.Forms.{boolean, ignored, mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.ValidateHelper.identifierMapping
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{Address, Enrolment, EnrolmentKey, Identifier, User}
import uk.gov.hmrc.domain.Nino

object UserForm {

  val none = "none"

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

  val form: Form[User] = Form[User](
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

}
