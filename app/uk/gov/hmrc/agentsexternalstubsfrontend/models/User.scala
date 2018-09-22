package uk.gov.hmrc.agentsexternalstubsfrontend.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino

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
  isPermanent: Option[Boolean] = None
)

object User {
  implicit val formats: Format[User] = Json.format[User]
}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None) {
  lazy val toEnrolmentKey: Option[EnrolmentKey] = identifiers.map(ii => EnrolmentKey(key, ii))
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
