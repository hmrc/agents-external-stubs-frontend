package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json.{Json, Reads}

case class CredentialsWithPlanetId(providerId: String, providerType: String, planetId: String)

object CredentialsWithPlanetId {
  val reads: Reads[CredentialsWithPlanetId] = Json.reads[CredentialsWithPlanetId]
}
