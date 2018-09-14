package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json.{Json, Reads}

case class EnrolmentInfo(enrolmentKey: EnrolmentKey, verifiers: Seq[KnownFact], user: Option[User], agents: Seq[User])

object EnrolmentInfo {
  implicit val reads: Reads[EnrolmentInfo] = Json.reads[EnrolmentInfo]
}
