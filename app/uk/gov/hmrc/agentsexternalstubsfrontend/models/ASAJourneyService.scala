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

import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}

enum EACDServiceKey(val key: String):
  case ASAAgent extends EACDServiceKey("HMRC-AS-AGENT")
  case VatAgent extends EACDServiceKey("HMCE-VAT-AGNT")
  case IrSa extends EACDServiceKey("IR-SA")
  case Itsa extends EACDServiceKey("HMRC-MTD-IT")
  case ItsaSupp extends EACDServiceKey("HMRC-MTD-IT-SUPP")
  case Vat extends EACDServiceKey("HMRC-MTD-VAT")
  case Ppt extends EACDServiceKey("HMRC-PPT-ORG")
  case CgtPd extends EACDServiceKey("HMRC-CGT-PD")
  case HmrcPt extends EACDServiceKey("HMRC-PT")
  case Trust extends EACDServiceKey("HMRC-TERS-ORG")
  case TrustNT extends EACDServiceKey("HMRC-TERSNT-ORG")
  case Cbc extends EACDServiceKey("HMRC-CBC-ORG")
  case CbcNonUk extends EACDServiceKey("HMRC-CBC-NONUK-ORG")
  case Pillar2 extends EACDServiceKey("HMRC-PILLAR2-ORG")

case class CustomerKnownFact(
  name: String,
  reads: Reads[String],
  sourcedFromUserRecord: Boolean
)

sealed trait ASAJourneyService {
  val friendlyName: String // to display on service select
  val affinityGroup: AffinityGroup
  val clientEacdServiceKey: EACDServiceKey
  val identifierName: String
  val identifierSourcedFromBpr: Boolean
  val identifierReadsPath: Reads[String]
  val createInvitationIdTypeName: String
  val createRelationshipIdTypeName: String
  val customerKnownFact: Option[CustomerKnownFact]
  val relServiceName: String
  def userOverride: User => User = user => user
}

case class SignedInUser(
  affinityGroup: Option[AffinityGroup],
  strideRole: Option[String] = None,
  services: List[EACDServiceKey],
  isAdmin: Boolean = true
)

sealed trait ASATestJourney {
  val id: String
  val signedInUser: SignedInUser
  val requiresServiceSelect: Boolean
}

sealed trait ASATestJourneyWithServiceSelection extends ASATestJourney
sealed trait ASATestJourneyWithoutServiceSelection extends ASATestJourney

case object ItsaService extends ASAJourneyService {
  override val friendlyName: String = "Itsa"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Itsa
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NI"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "postcode",
        reads = (JsPath \ "address" \ "postcode").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = EACDServiceKey.Itsa.key
}

case object ItsaSuppService extends ASAJourneyService {
  override val friendlyName: String = "ItsaSupporting"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Itsa
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NI"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "postcode",
        reads = (JsPath \ "address" \ "postcode").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = EACDServiceKey.ItsaSupp.key
}

case object ItsaOverseasService extends ASAJourneyService {
  override val friendlyName: String = "ItsaOverseas"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Itsa
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NI"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "countryCode",
        reads = (JsPath \ "address" \ "countryCode").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = EACDServiceKey.Itsa.key
  override def userOverride: User => User = user =>
    user.copy(address = user.address.map(_.copy(countryCode = Some("FR"))))
}

case object AltItsaService extends ASAJourneyService {
  override val friendlyName: String = "AltItsa"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.IrSa
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NI"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "postcode",
        reads = (JsPath \ "address" \ "postcode").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = EACDServiceKey.Itsa.key
}

case object AltItsaSuppService extends ASAJourneyService {
  override val friendlyName: String = "AltItsaSupporting"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.IrSa
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NI"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "postcode",
        reads = (JsPath \ "address" \ "postcode").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = EACDServiceKey.ItsaSupp.key
}

case object IRVService extends ASAJourneyService {
  override val friendlyName: String = "IncomeRecordViewer"
  override val affinityGroup: AffinityGroup = Individual
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.HmrcPt
  override val identifierName: String = "nino" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "nino").read[String]
  override val createInvitationIdTypeName: String = "ni"
  override val createRelationshipIdTypeName: String = "NINO"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "d.o.b.",
        (JsPath \ "dateOfBirth").read[String],
        sourcedFromUserRecord = true
      )
    )
  override val relServiceName: String = "PERSONAL-INCOME-RECORD"
}

case object VatService extends ASAJourneyService {
  override val friendlyName: String = "Vat"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Vat
  override val identifierName: String = "vrn" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "vrn").read[String]
  override val createInvitationIdTypeName: String = "vrn"
  override val createRelationshipIdTypeName: String = "VRN"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "dateOfRegistration",
        (JsPath \ "approvedInformation" \ "customerDetails" \ "effectiveRegistrationDate").read[String],
        sourcedFromUserRecord = false
      )
    )
  override val relServiceName: String = EACDServiceKey.Vat.key
}

case object PptService extends ASAJourneyService {
  override val friendlyName: String = "PlasticPackagingTax"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Ppt
  override val identifierName: String = "pptReference" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "pptReference").read[String]
  override val createInvitationIdTypeName: String = "EtmpRegistrationNumber"
  override val createRelationshipIdTypeName: String = "EtmpRegistrationNumber"
  override val customerKnownFact: Option[CustomerKnownFact] = Some(
    CustomerKnownFact(
      name = "appliedOn",
      (JsPath \ "legalEntityDetails" \ "dateOfApplication").read[String],
      sourcedFromUserRecord = false
    )
  )
  override val relServiceName: String = EACDServiceKey.Ppt.key
}

case object CgtPdService extends ASAJourneyService {
  override val friendlyName: String = "CapitalGainsTaxProperty"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.CgtPd
  override val identifierName: String = "cgtPdRef" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "cgtPdRef").read[String]
  override val createInvitationIdTypeName: String = "CGTPDRef"
  override val createRelationshipIdTypeName: String = "CGTPDRef"
  override val customerKnownFact: Option[CustomerKnownFact] = Some(
    CustomerKnownFact(
      name = "postalCode",
      ((JsPath \ "businessData")(0) \ "businessAddressDetails" \ "postalCode").read[String],
      sourcedFromUserRecord = false
    )
  )
  override val relServiceName: String = EACDServiceKey.CgtPd.key
}

case object TaxableTrustService extends ASAJourneyService {
  override val friendlyName: String = "TaxableTrust"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Trust
  override val identifierName: String = "utr" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "assignedPrincipalEnrolments")(0).read[String]
  override val createInvitationIdTypeName: String = "utr"
  override val createRelationshipIdTypeName: String = "SAUTR"
  override val customerKnownFact: Option[CustomerKnownFact] = None
  override val relServiceName: String = EACDServiceKey.Trust.key
}

case object NonTaxableTrustService extends ASAJourneyService {
  override val friendlyName: String = "NonTaxableTrust"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.TrustNT
  override val identifierName: String = "urn" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = false
  override val identifierReadsPath: Reads[String] = (JsPath \ "assignedPrincipalEnrolments")(0).read[String]
  override val createInvitationIdTypeName: String = "urn"
  override val createRelationshipIdTypeName: String = "URN"
  override val customerKnownFact: Option[CustomerKnownFact] = None
  override val relServiceName: String = EACDServiceKey.TrustNT.key
}

case object CBCService extends ASAJourneyService {
  override val friendlyName: String = "CountryByCountry"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Cbc
  override val identifierName: String = "cbcId" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "cbcId").read[String]
  override val createInvitationIdTypeName: String = "cbcId"
  override val createRelationshipIdTypeName: String = "cbcId"
  override val customerKnownFact: Option[CustomerKnownFact] = Some(
    CustomerKnownFact(
      name = "email",
      ((JsPath \ "primaryContact")(0) \ "email").read[String],
      sourcedFromUserRecord = false
    )
  )
  override val relServiceName: String = EACDServiceKey.Cbc.key
}

case object CBCNonUkService extends ASAJourneyService {
  override val friendlyName: String = "CountryByCountryNonUk"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.CbcNonUk
  override val identifierName: String = "cbcId" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "cbcId").read[String]
  override val createInvitationIdTypeName: String = "cbcId"
  override val createRelationshipIdTypeName: String = "cbcId"
  override val customerKnownFact: Option[CustomerKnownFact] = Some(
    CustomerKnownFact(
      name = "email",
      ((JsPath \ "primaryContact")(0) \ "email").read[String],
      sourcedFromUserRecord = false
    )
  )
  override val relServiceName: String = EACDServiceKey.CbcNonUk.key
}

case object Pillar2Service extends ASAJourneyService {
  override val friendlyName: String = "Pillar2"
  override val affinityGroup: AffinityGroup = Organisation
  override val clientEacdServiceKey: EACDServiceKey = EACDServiceKey.Pillar2
  override val identifierName: String = "plrReference" // to display on test data page
  override val identifierSourcedFromBpr: Boolean = true
  override val identifierReadsPath: Reads[String] = (JsPath \ "plrReference").read[String]
  override val createInvitationIdTypeName: String = "PLRID"
  override val createRelationshipIdTypeName: String = "PLRID"
  override val customerKnownFact: Option[CustomerKnownFact] =
    Some(
      CustomerKnownFact(
        name = "appliedOn",
        (JsPath \ "upeDetails" \ "registrationDate").read[String],
        sourcedFromUserRecord = false
      )
    )
  override val relServiceName: String = EACDServiceKey.Pillar2.key
}

//requires service selection
case object CreateInvitation extends ASATestJourneyWithServiceSelection {
  override val id: String = "create-invitation"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent))
  override val requiresServiceSelect: Boolean = true
}

//no service selection
case object MytaInd extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "myta-ind"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Individual), services = List(EACDServiceKey.Itsa, EACDServiceKey.HmrcPt))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object MytaOrg extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "myta-org"
  override val signedInUser: SignedInUser =
    SignedInUser(
      affinityGroup = Some(Organisation),
      services = List(
        EACDServiceKey.Ppt,
        EACDServiceKey.CgtPd,
        EACDServiceKey.Pillar2,
        EACDServiceKey.Vat,
        EACDServiceKey.Trust,
        EACDServiceKey.TrustNT,
        EACDServiceKey.Cbc
      )
    )
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object UkSubscription extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "uk-subscription"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.VatAgent))
  override val requiresServiceSelect: Boolean = false

}

//requires service selection
case object HmrcLedDeauth extends ASATestJourneyWithServiceSelection {
  override val id: String = "hmrc-led-deauth"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = None, strideRole = Some("maintain_agent_relationships"), services = List().empty)
  override val requiresServiceSelect: Boolean = true

}

//requires service selection
case object AgentLedDeauth extends ASATestJourneyWithServiceSelection {
  override val id: String = "agent-led-deauth"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent))
  override val requiresServiceSelect: Boolean = true

}

//no service selection
case object Track extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "track"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object AccessGroups extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "access-groups"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object MmtarProvideDetails extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "provide-details"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Individual), services = List(EACDServiceKey.Itsa))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object AsaDashboardAdminUser extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "manage-account"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object AsaDashboardStandardUser extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "your-account"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.ASAAgent), isAdmin = false)
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object MmtarStartRegistration extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "agent-registration"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List(EACDServiceKey.VatAgent))
  override val requiresServiceSelect: Boolean = false

}

//no service selection
case object OverseasApplication extends ASATestJourneyWithoutServiceSelection {
  override val id: String = "overseas-application"
  override val signedInUser: SignedInUser =
    SignedInUser(affinityGroup = Some(Agent), services = List())
  override val requiresServiceSelect: Boolean = false

}

object ASAJourneyService {

  private val asaJourneyServices: List[ASAJourneyService] = List(
    ItsaService,
    ItsaSuppService,
    ItsaOverseasService,
    AltItsaService,
    AltItsaSuppService,
    IRVService,
    VatService,
    PptService,
    CgtPdService,
    TaxableTrustService,
    NonTaxableTrustService,
    CBCService,
    CBCNonUkService,
    Pillar2Service
  )

  def asaJourneyForId(id: String): Option[ASATestJourney] =
    asaJourneys.find(_.id == id)

  def asaJourneyServiceForEacdKey(key: String): Option[ASAJourneyService] =
    asaJourneyServices.find(
      _.clientEacdServiceKey == eacdServices
        .find(_.key == key)
        .getOrElse(throw new RuntimeException("could not find a eacdService with that key"))
    )

  def asaJourneyServiceForFriendlyName(friendlyName: String): ASAJourneyService =
    asaJourneyServices
      .find(_.friendlyName == friendlyName)
      .getOrElse(throw new RuntimeException("no service by that friendly name."))

  val asaJourneyServicesForCreateInvitation: List[ASAJourneyService] = List(
    ItsaService,
    AltItsaService,
    IRVService,
    VatService,
    PptService,
    CgtPdService,
    TaxableTrustService,
    NonTaxableTrustService,
    CBCService,
    CBCNonUkService,
    Pillar2Service
  )

  val asaJourneyServicesForDeauth: List[ASAJourneyService] = asaJourneyServices

  val asaJourneys: List[ASATestJourney] =
    List(
      CreateInvitation,
      AgentLedDeauth,
      HmrcLedDeauth,
      MytaInd,
      MytaOrg,
      UkSubscription,
      AccessGroups,
      Track,
      AsaDashboardAdminUser,
      AsaDashboardStandardUser,
      MmtarProvideDetails,
      MmtarStartRegistration,
      OverseasApplication
    )
  private val eacdServices: List[EACDServiceKey] =
    List(
      EACDServiceKey.ASAAgent,
      EACDServiceKey.VatAgent,
      EACDServiceKey.IrSa,
      EACDServiceKey.HmrcPt,
      EACDServiceKey.Itsa,
      EACDServiceKey.ItsaSupp,
      EACDServiceKey.Vat,
      EACDServiceKey.Ppt,
      EACDServiceKey.CgtPd,
      EACDServiceKey.Trust,
      EACDServiceKey.TrustNT,
      EACDServiceKey.Cbc,
      EACDServiceKey.CbcNonUk,
      EACDServiceKey.Pillar2
    )

}
