package uk.gov.hmrc.agentsexternalstubsfrontend.models

import play.api.libs.json.Json

case class AgentsExternalStubsFrontendModel(
  parameter1: String,
  parameter2: Option[String],
  telephoneNumber: Option[String],
  emailAddress: Option[String])

object AgentsExternalStubsFrontendModel {
  implicit val modelFormat = Json.format[AgentsExternalStubsFrontendModel]
}
