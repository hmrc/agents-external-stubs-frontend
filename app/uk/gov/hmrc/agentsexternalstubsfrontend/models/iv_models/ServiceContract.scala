package uk.gov.hmrc.agentsexternalstubsfrontend.models.iv_models

import play.api.libs.json._
import uk.gov.hmrc.play.json.Mappings
import play.api.libs.functional.syntax._

case class ServiceContract(origin: Option[String], completionURL: String, failureURL: String, confidenceLevel: Int)

object ServiceContract {
  implicit val format: OFormat[ServiceContract] = Json.format[ServiceContract]
}

sealed trait JourneyType

object JourneyType {
  object UpliftNino extends JourneyType
  object UpliftNoNino extends JourneyType

  val mapping = Mappings.mapEnum(UpliftNino, UpliftNoNino)

  implicit val journeyFormat = mapping.jsonFormat
}

case class Journey(origin: String, completionURL: String, failureURL: String, confidenceLevel: Int)

object Journey {

  implicit val reads: Reads[Journey] = (
    (__ \ "serviceContract" \ "origin").read[String] and
      (__ \ "serviceContract" \ "completionURL").read[String] and
      (__ \ "serviceContract" \ "failureURL").read[String] and
      (__ \ "serviceContract" \ "confidenceLevel").read[Int]
  )(Journey.apply _)
}
