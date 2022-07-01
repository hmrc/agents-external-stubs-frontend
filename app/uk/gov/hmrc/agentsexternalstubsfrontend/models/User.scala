/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, JsPath, Json, Reads, Writes}
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

import scala.io.{BufferedSource, Source}

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  enrolments: Option[Enrolments] = None,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  isPermanent: Option[Boolean] = None,
  recordIds: Option[Seq[String]] = None,
  address: Option[Address] = None,
  strideRoles: Seq[String] = Seq.empty,
  suspendedRegimes: Option[Set[String]] = None
) {

  def isEnrolledFor(service: String): Boolean =
    enrolments.exists(_.principal.exists(_.key == service))

  val defaultProviderType: String =
    if (strideRoles.nonEmpty) AuthProvider.PrivilegedApplication else AuthProvider.GovernmentGateway

  def updatePrincipalEnrolments(f: Seq[Enrolment] => Seq[Enrolment]): User =
    this.copy(enrolments =
      Some(
        this.enrolments
          .getOrElse(Enrolments.none)
          .copy(principal = f(this.enrolments.getOrElse(Enrolments.none).principal))
      )
    )
  def updateDelegatedEnrolments(f: Seq[Enrolment] => Seq[Enrolment]): User =
    this.copy(enrolments =
      Some(
        this.enrolments
          .getOrElse(Enrolments.none)
          .copy(delegated = f(this.enrolments.getOrElse(Enrolments.none).delegated))
      )
    )
  def updateAssignedEnrolments(f: Seq[EnrolmentKey] => Seq[EnrolmentKey]): User =
    this.copy(enrolments =
      Some(
        this.enrolments
          .getOrElse(Enrolments.none)
          .copy(assigned = f(this.enrolments.getOrElse(Enrolments.none).assigned))
      )
    )

}

object User {

  val Individual = "Individual"

  implicit val formats: Format[User] = Json.format[User]
}

case class Address(
  line1: Option[String] = None,
  line2: Option[String] = None,
  line3: Option[String] = None,
  line4: Option[String] = None,
  postcode: Option[String] = None,
  countryCode: Option[String] = None
) {

  def isUKAddress: Boolean = countryCode.contains("GB")

}

object Address {
  implicit lazy val formats: Format[Address] = Json.format[Address]
}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None) {
  lazy val toEnrolmentKey: Option[EnrolmentKey] = identifiers.map(ii => EnrolmentKey(key, ii))

  def identifiersValues: Seq[String] = identifiers.map(_.map(_.value)).getOrElse(Seq.empty)
}

object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}

case class Identifier(key: String, value: String) {
  override def toString: String = s"${key.toUpperCase}~$value"
}

object Identifier {
  implicit val format: Format[Identifier] = Json.format[Identifier]
  implicit val ordering: Ordering[Identifier] = Ordering.by(_.key)
}

object ConfidenceLevel {
  val values: Seq[(String, String)] = Seq("0" -> "none", "50" -> "50", "200" -> "200", "250" -> "250")
}

object CredStrength {
  val values = Seq("none", "strong", "weak")
}

object AffinityGroup {
  val values = Seq("none", "Individual", "Organisation", "Agent")
}

object CredentialRole {
  val values = Seq("none", "User", "Assistant", "Admin")
}

case class Country(name: String, code: String, phone_code: String)

object Country {

  implicit val reads: Reads[Country] = Json.reads[Country]

  lazy val countriesSource: BufferedSource =
    Source.fromInputStream(getClass.getResourceAsStream("/countries.json"), "utf-8")

  lazy val countries: Seq[Country] = Json.parse(countriesSource.mkString).as[Seq[Country]]

  lazy val byCode: Map[String, Country] = countries.map(c => c.code -> c).toMap
}

object CountryCodes {

  lazy val values: Seq[(String, String)] = Country.countries.map(c => c.code -> c.name)

}

case class Enrolments(
  principal: Seq[Enrolment] = Seq.empty,
  delegated: Seq[Enrolment] = Seq.empty,
  assigned: Seq[EnrolmentKey] = Seq.empty
)

object Enrolments {
  import play.api.libs.functional.syntax._
  implicit val reads: Reads[Enrolments] = (
    (JsPath \ "principal").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "delegated").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "assigned").readNullable[Seq[EnrolmentKey]].map(_.getOrElse(Seq.empty))
  )(Enrolments.apply _)
  implicit val writes: Writes[Enrolments] = Json.writes[Enrolments]

  val none = Enrolments(Seq.empty, Seq.empty, Seq.empty)
}
