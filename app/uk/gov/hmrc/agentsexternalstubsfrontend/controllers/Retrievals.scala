package uk.gov.hmrc.agentsexternalstubsfrontend.controllers
import uk.gov.hmrc.agentsexternalstubsfrontend.models.CredentialsWithPlanetId
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, SimpleRetrieval}

object Retrievals {

  val credentialsWithPlanetId: Retrieval[CredentialsWithPlanetId] =
    SimpleRetrieval("credentials", CredentialsWithPlanetId.reads)
}
