package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json.{Json, Reads}

case class KnownFact(key: String, value: String) {
  override def toString: String = s"${key.toUpperCase}~$value"
}

object KnownFact {
  implicit val reads: Reads[KnownFact] = Json.reads[KnownFact]
  implicit val ordering: Ordering[KnownFact] = Ordering.by(_.key)
}
