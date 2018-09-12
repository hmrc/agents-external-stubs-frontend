package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json.{JsObject, Json, Reads}

case class Records(
  BusinessPartnerRecord: Option[Seq[JsObject]],
  VatCustomerInformationRecord: Option[Seq[JsObject]],
  LegacyAgentRecord: Option[Seq[JsObject]],
  BusinessDetailsRecord: Option[Seq[JsObject]],
  LegacyRelationshipRecord: Option[Seq[JsObject]])

object Records {
  implicit val reads: Reads[Records] = Json.reads[Records]
}
