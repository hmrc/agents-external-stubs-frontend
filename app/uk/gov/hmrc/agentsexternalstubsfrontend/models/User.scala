package uk.gov.hmrc.agentsexternalstubsfrontend.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json, Reads}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.CountryCodes.values
import uk.gov.hmrc.domain.Nino

import scala.io.{BufferedSource, Source}

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  principalEnrolments: Option[Seq[Enrolment]] = None,
  delegatedEnrolments: Option[Seq[Enrolment]] = None,
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
    principalEnrolments.exists(_.exists(_.key == service))

  val defaultProviderType: String =
    if (strideRoles.nonEmpty) AuthProvider.PrivilegedApplication else AuthProvider.GovernmentGateway
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
  countryCode: Option[String] = None) {

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
  val values: Seq[(String, String)] = Seq("0" -> "none", "50" -> "50", "100" -> "100", "200" -> "200", "300" -> "300")
}

object CredStrength {
  val values = Seq("none", "strong", "weak")
}

object AffinityGroup {
  val values = Seq("none", "Individual", "Organisation", "Agent")
}

object CredentialRole {
  val values = Seq("none", "Admin", "User", "Assistant")
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
