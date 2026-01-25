/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.Reads
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AsaTestJourney.{CapitalGains, Cbc, Itsa, Pillar2, Ppt, Vat}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}

case class CustomerKnownFact(
  name: String,
  reads: Reads[String],
  sourcedFromUserRecord: Boolean
)

case class CustomerIdentifier(
  name: String,
  sourcedFromBpr: Boolean,
  readsPath: Reads[String]
)

case class Relationship(
  clientIdTypeForRel: String,
  delegatedServiceKey: String
)

case class Client(
  affinityGroup: AffinityGroup,
  principalServiceKey: Option[String],
  identifierName: String,
  identifierSourcedFromBpr: Boolean,
  identifierReadsPath: Reads[String]
)

sealed abstract class AsaService(
  val id: String,
  val friendlyName: String,
  val client: Client,
  val relationship: Relationship,
  val knownFact: Option[CustomerKnownFact]
) {

  override def toString: String = this.friendlyName

}

import play.api.libs.json._

object AsaTestJourney {

  case object AltItsa
      extends AsaService(
        "AltItsa",
        "Alt-ITSA",
        Client(Individual, Some("IR-SA"), "nino", identifierSourcedFromBpr = false, (JsPath \ "nino").read[String]),
        Relationship("ni", "HMRC-MTD-IT"),
        Some(
          CustomerKnownFact(
            name = "postcode",
            (JsPath \ "address" \ "postcode").read[String],
            sourcedFromUserRecord = true
          )
        )
      )

  case object AltItsaSupp
      extends AsaService(
        "AltItsaSupp",
        "Alt-ITSA(supporting)",
        Client(Individual, Some("IR-SA"), "nino", identifierSourcedFromBpr = false, (JsPath \ "nino").read[String]),
        Relationship("ni", "HMRC-MTD-IT-SUPP"),
        Some(
          CustomerKnownFact(
            name = "postcode",
            (JsPath \ "address" \ "postcode").read[String],
            sourcedFromUserRecord = true
          )
        )
      )

  case object Itsa
      extends AsaService(
        "Itsa",
        "ITSA",
        Client(
          Individual,
          Some("HMRC-MTD-IT"),
          "nino",
          identifierSourcedFromBpr = false,
          (JsPath \ "nino").read[String]
        ),
        Relationship("ni", "HMRC-MTD-IT"),
        Some(
          CustomerKnownFact(
            name = "postcode",
            (JsPath \ "address" \ "postcode").read[String],
            sourcedFromUserRecord = true
          )
        )
      )

  case object ItsaSupp
      extends AsaService(
        "ItsaSupp",
        "ITSA(supporting)",
        Client(
          Individual,
          Some("HMRC-MTD-IT"),
          "nino",
          identifierSourcedFromBpr = false,
          (JsPath \ "nino").read[String]
        ),
        Relationship("ni", "HMRC-MTD-IT-SUPP"),
        Some(
          CustomerKnownFact(
            name = "postcode",
            (JsPath \ "address" \ "postcode").read[String],
            sourcedFromUserRecord = true
          )
        )
      )

  case object PersonalIncomeRecord
      extends AsaService(
        "PERSONAL-INCOME-RECORD",
        "PersonalIncomeRecord",
        Client(Individual, Some("HMRC-PT"), "nino", identifierSourcedFromBpr = false, (JsPath \ "nino").read[String]),
        Relationship("NINO", "PERSONAL-INCOME-RECORD"),
        Some(
          CustomerKnownFact(
            name = "d.o.b.",
            (JsPath \ "dateOfBirth").read[String],
            sourcedFromUserRecord = true
          )
        )
      )

  case object Vat
      extends AsaService(
        "VAT",
        "VAT",
        Client(
          Organisation,
          Some("HMRC-MTD-VAT"),
          "vrn",
          identifierSourcedFromBpr = true,
          (JsPath \ "vrn").read[String]
        ),
        Relationship("VRN", "HMRC-MTD-VAT"),
        Some(
          CustomerKnownFact(
            name = "dateOfRegistration",
            (JsPath \ "approvedInformation" \ "customerDetails" \ "effectiveRegistrationDate").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  case object Trust
      extends AsaService(
        "TERS",
        "TaxableTrust",
        Client(
          Organisation,
          Some("HMRC-TERS-ORG"),
          "utr",
          identifierSourcedFromBpr = false,
          (JsPath \ "assignedPrincipalEnrolments")(0).read[String]
        ),
        Relationship("SAUTR", "HMRC-TERS-ORG"),
        None
      )

  case object TrustNT
      extends AsaService(
        "TERSNT",
        "NonTaxableTrust",
        Client(
          Organisation,
          Some("HMRC-TERSNT-ORG"),
          "urn",
          identifierSourcedFromBpr = false,
          (JsPath \ "assignedPrincipalEnrolments")(0).read[String]
        ),
        Relationship("URN", "HMRC-TERSNT-ORG"),
        None
      )

  case object CapitalGains
      extends AsaService(
        "CGT",
        "CapitalGainsTax",
        Client(
          Organisation,
          Some("HMRC-CGT-PD"),
          "cgtPdRef",
          identifierSourcedFromBpr = true,
          (JsPath \ "cgtPdRef").read[String]
        ),
        Relationship("CGTPDRef", "HMRC-CGT-PD"),
        Some(
          CustomerKnownFact(
            name = "postalCode",
            ((JsPath \ "businessData")(0) \ "businessAddressDetails" \ "postalCode").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  case object Ppt
      extends AsaService(
        "PPT",
        "PlasticPackagingTax",
        Client(
          Organisation,
          Some("HMRC-PPT-ORG"),
          "pptReference",
          identifierSourcedFromBpr = true,
          (JsPath \ "pptReference").read[String]
        ),
        Relationship("EtmpRegistrationNumber", "HMRC-PPT-ORG"),
        Some(
          CustomerKnownFact(
            name = "appliedOn",
            (JsPath \ "legalEntityDetails" \ "dateOfApplication").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  case object Cbc
      extends AsaService(
        "CBC",
        "CountryByCountry",
        Client(
          Organisation,
          Some("HMRC-CBC-ORG"),
          "cbcId",
          identifierSourcedFromBpr = true,
          (JsPath \ "cbcId").read[String]
        ),
        Relationship("cbcId", "HMRC-CBC-ORG"),
        Some(
          CustomerKnownFact(
            name = "email",
            ((JsPath \ "primaryContact")(0) \ "email").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  case object CbcNonUk
      extends AsaService(
        "CBCNonUk",
        "CountryByCountryNonUK",
        Client(
          Organisation,
          Some("HMRC-CBC-NONUK-ORG"),
          "cbcId",
          identifierSourcedFromBpr = true,
          (JsPath \ "cbcId").read[String]
        ),
        Relationship("cbcId", "HMRC-CBC-NONUK-ORG"),
        Some(
          CustomerKnownFact(
            name = "email",
            ((JsPath \ "primaryContact")(0) \ "email").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  case object Pillar2
      extends AsaService(
        "Pillar2",
        "Pillar2",
        Client(
          Organisation,
          Some("HMRC-PILLAR2-ORG"),
          "plrReference",
          identifierSourcedFromBpr = true,
          (JsPath \ "plrReference").read[String]
        ),
        Relationship("PLRID", "HMRC-PILLAR2-ORG"),
        Some(
          CustomerKnownFact(
            name = "appliedOn",
            (JsPath \ "upeDetails" \ "registrationDate").read[String],
            sourcedFromUserRecord = false
          )
        )
      )

  val supportedServices: Seq[AsaService] = Seq(
    AltItsa,
    AltItsaSupp,
    Itsa,
    ItsaSupp,
    Vat,
    PersonalIncomeRecord,
    Trust,
    TrustNT,
    CapitalGains,
    Ppt,
    Cbc,
    CbcNonUk,
    Pillar2
  )

  val createInvitationServices: Seq[AsaService] = Seq(
    AltItsa,
    Itsa,
    Vat,
    PersonalIncomeRecord,
    Trust,
    TrustNT,
    CapitalGains,
    Ppt,
    Cbc,
    CbcNonUk,
    Pillar2
  )

  def createInvitationServicesForIndividuals(altItsa: Boolean = false): Seq[AsaService] = createInvitationServices
    .filter((service: AsaService) => service.client.affinityGroup == Individual) diff (if (altItsa) List(Itsa)
                                                                                       else List(AltItsa))

  def findById(id: String): Option[AsaService] = supportedServices.find(_.id == id)

  def forId(id: String): AsaService = findById(id).getOrElse(throw new Exception("Not a valid service id: " + id))

  def apply(id: String): AsaService = forId(id)
  def unapply(service: AsaService): Option[String] = Some(service.id)

  def isTrustService(asaService: AsaService): Boolean =
    List(Trust, TrustNT).contains(asaService)

  def isAltItsaOrIRVService(asaService: AsaService): Boolean =
    List(AltItsa, AltItsaSupp, PersonalIncomeRecord).contains(asaService)

  def isNinoService(asaService: AsaService): Boolean =
    List(AltItsa, AltItsaSupp, Itsa, ItsaSupp, PersonalIncomeRecord).contains(asaService)

  def toJourney(j: String): AsaTestJourney =
    j match {
      case "create-invitation" => CreateInvitation
      case "agent-led-deauth"  => AgentLedDeauth
      case "hmrc-led-deauth"   => HmrcLedDeauth
      case "myta-ind"          => MytaInd
      case "myta-org"          => MytaOrg
      case "track"             => Track
      case "access-groups"     => AccessGroups
      case "provide-details"   => MmtarProvideDetails
      case "uk-subscription"   => UkSubscription
    }

  val asaTestJourneys: List[AsaTestJourney] = List(
    CreateInvitation,
    AgentLedDeauth,
    HmrcLedDeauth,
    MytaInd,
    MytaOrg,
    Track,
    AccessGroups,
    MmtarProvideDetails,
    UkSubscription
  )

}

sealed trait AsaTestJourney {
  val requiresServiceSelect: Boolean
  val signedInUserAffinityGroup: Option[AffinityGroup]
  val signedInUserServices: List[AsaService]

}

sealed trait JourneyWithServiceSelected extends AsaTestJourney
sealed trait JourneyWithoutServiceSelected extends AsaTestJourney

case object CreateInvitation extends JourneyWithServiceSelected {
  override val requiresServiceSelect: Boolean = true
  override val toString: String = "create-invitation"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Agent)
  override val signedInUserServices: List[AsaService] = List().empty
}

case object AgentLedDeauth extends JourneyWithServiceSelected {
  override val requiresServiceSelect: Boolean = true
  override val toString: String = "agent-led-deauth"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Agent)
  override val signedInUserServices: List[AsaService] = List().empty
}

case object HmrcLedDeauth extends JourneyWithServiceSelected {
  override val requiresServiceSelect: Boolean = true
  override val toString: String = "hmrc-led-deauth"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = None
  override val signedInUserServices: List[AsaService] = List().empty
}

case object MytaInd extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "myta-ind"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Individual)
  override val signedInUserServices: List[AsaService] = List(Itsa)
}

case object MytaOrg extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "myta-org"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Organisation)
  override val signedInUserServices: List[AsaService] = List(
    Vat,
    CapitalGains,
    Ppt,
    Pillar2,
    Cbc
  )
}

case object Track extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "track"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Agent)
  override val signedInUserServices: List[AsaService] = List().empty
}

case object AccessGroups extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "access-groups"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Agent)
  override val signedInUserServices: List[AsaService] = List().empty
}

case object MmtarProvideDetails extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "provide-details"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Individual)
  override val signedInUserServices: List[AsaService] = List(Itsa)
}

case object UkSubscription extends JourneyWithoutServiceSelected {
  override val requiresServiceSelect: Boolean = false
  override val toString: String = "uk-subscription"
  override val signedInUserAffinityGroup: Option[AffinityGroup] = Some(Agent)
  override val signedInUserServices: List[AsaService] = List().empty
}
