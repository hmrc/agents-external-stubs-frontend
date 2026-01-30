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

package uk.gov.hmrc.agentsexternalstubsfrontend.services

import com.google.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.{AgentClientRelationshipsConnector, AgentRegistrationConnector, AgentsExternalStubsConnector}
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.agentsexternalstubsfrontend.util.RequestSupport._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AsaJourneySetupService @Inject() (
  agentsExternalStubsConnector: AgentsExternalStubsConnector,
  agentRegistrationConnector: AgentRegistrationConnector,
  acrConnector: AgentClientRelationshipsConnector
)(implicit ec: ExecutionContext) {

  def setupMainUser(journey: ASATestJourney)(implicit rh: RequestHeader) =
    for {
      authSession <- agentsExternalStubsConnector.signIn()
      hc = HeaderCarrier(
             sessionId = Some(SessionId(authSession.sessionId))
           )
      _ <- agentsExternalStubsConnector.removeUser(authSession.userId)(hc, ec)

      user <- createUser(
                affinityGroup = journey.signedInUser.affinityGroup,
                services = journey.signedInUser.services,
                strideRole = journey.signedInUser.strideRole
              )(hc)
      authenticatedSession <- agentsExternalStubsConnector.signIn(
                                SignInRequest(
                                  userId = user.userId,
                                  planetId = authSession.planetId,
                                  providerType =
                                    if (journey.signedInUser.affinityGroup.isEmpty)
                                      AuthProvider.PrivilegedApplication
                                    else AuthProvider.GovernmentGateway
                                )
                              )(hc, ec)
    } yield authenticatedSession


  def setupDataForJourneyWithServiceSelected(
    journey: ASATestJourneyWithServiceSelection,
    asaJourneyService: ASAJourneyService
  )(implicit
    rh: RequestHeader
  ): Future[String] =
    journey match {
      case CreateInvitation =>
        for {
          customer <- createUser(
                        affinityGroup = Some(asaJourneyService.affinityGroup),
                        services = List(asaJourneyService.clientEacdServiceKey)
                      )
          testData <- extractTestDataFromUser(customer, asaJourneyService)
        } yield testData

      case AgentLedDeauth =>
        for {
          customer <- createUser(
                        affinityGroup = Some(asaJourneyService.affinityGroup),
                        services = List(asaJourneyService.clientEacdServiceKey)
                      )
          agent <- getSignedInUser()
          arn = extractArnFromAgent(agent)
          clientId = extractOneIdentifier(customer)
          _ <- acrConnector.testOnlyCreateRelationship(
                 arn,
                 clientId,
                 asaJourneyService.relServiceName,
                 asaJourneyService.createRelationshipIdTypeName
               )
          testData <- extractTestDataFromUser(customer, asaJourneyService)
        } yield testData

      case HmrcLedDeauth =>
        for {
          customer <- createUser(
                        affinityGroup = Some(asaJourneyService.affinityGroup),
                        services = List(asaJourneyService.clientEacdServiceKey)
                      )
          agent <- createAgentUser()
          arn = extractArnFromAgent(agent)
          clientId = extractOneIdentifier(customer)
          _ <- acrConnector.testOnlyCreateRelationship(
                 arn,
                 clientId,
                 asaJourneyService.relServiceName,
                 asaJourneyService.createRelationshipIdTypeName
               )
          testData <- extractTestDataFromUser(customer, asaJourneyService)
        } yield testData
    }

  def setupDataForJourneyWithoutServiceSelected(
    journey: ASATestJourneyWithoutServiceSelection
  )(implicit rh: RequestHeader): Future[Option[String]] =
    journey match {
      case MytaInd =>
        for {
          ind    <- getSignedInUser()
          agent1 <- createAgentUser()
          nino = ind.nino.get.value
          arn1 = extractArnFromAgent(agent1)
          agent2 <- createAgentUser()
          arn2 = extractArnFromAgent(agent2)
          _ <- acrConnector.testOnlyJourneySetup(
                 JourneySetupRequest(
                   invitations = Seq(
                     JourneySetupInvitation(
                       arn1,
                       nino,
                       "ni",
                       ind.name.getOrElse(""),
                       "HMRC-MTD-IT",
                       Some("personal")
                     ),
                     JourneySetupInvitation(
                       arn1,
                       nino,
                       "ni",
                       ind.name.getOrElse(""),
                       "PERSONAL-INCOME-RECORD",
                       Some("personal")
                     ),
                     JourneySetupInvitation(
                       arn2,
                       nino,
                       "ni",
                       ind.name.getOrElse(""),
                       "HMRC-MTD-IT-SUPP",
                       Some("personal")
                     ),
                     JourneySetupInvitation(
                       arn2,
                       nino,
                       "ni",
                       ind.name.getOrElse(""),
                       "PERSONAL-INCOME-RECORD",
                       Some("personal")
                     )
                   )
                 )
               )
        } yield None

      case MytaOrg =>
        for {
          org   <- getSignedInUser()
          agent <- createAgentUser()
          arn = extractArnFromAgent(agent)
          invitations = journey.signedInUser.services.map { eacdServiceKey =>
                          val service = ASAJourneyService
                            .asaJourneyServiceForEacdKey(eacdServiceKey.key)
                            .getOrElse(throw new RuntimeException("no service found."))
                          JourneySetupInvitation(
                            arn,
                            extractIdentifier(org, service.clientEacdServiceKey.key),
                            service.createInvitationIdTypeName,
                            org.name.getOrElse(""),
                            service.relServiceName,
                            Some("business")
                          )
                        }
          _ <- acrConnector.testOnlyJourneySetup(
                 JourneySetupRequest(invitations = invitations)
               )
        } yield None
      case Track =>
        for {
          agent <- getSignedInUser()
          arn = extractArnFromAgent(agent)
          invitations <-
            Future.sequence(
              ASAJourneyService.asaJourneyServicesForCreateInvitation.map(service =>
                createUser(Some(service.affinityGroup), List(service.clientEacdServiceKey)).map(user =>
                  JourneySetupInvitation(
                    arn = arn,
                    clientId = extractOneIdentifier(user),
                    suppliedClientIdType = service.createInvitationIdTypeName,
                    clientName = user.name.getOrElse(""),
                    service = service.relServiceName,
                    clientType = Some(if (service.affinityGroup == Individual) "personal" else "business")
                  )
                )
              )
            )
          _ <- acrConnector.testOnlyJourneySetup(JourneySetupRequest(invitations = invitations))
        } yield None

      case UkSubscription =>
        val utrReads: Reads[String] = (JsPath \ "utr").read[String]
        val postcodeReads: Reads[String] = (JsPath \ "addressDetails" \ "postalCode").read[String]
        val crnReads: Reads[String] = (JsPath \ "crn").read[String]
        for {
          cleanAgent <- createAgentUser(None)
          bpr        <- agentsExternalStubsConnector.getRecord(cleanAgent.recordIds.get.head)
          utr = bpr.as[String](utrReads)
          postcode = bpr.as[String](postcodeReads)
          crn = bpr.as[String](crnReads)
        } yield Some(s"""{"utr": "$utr", "postcode": "${easyCopy(postcode)}", "crn": "$crn" }""")

      case AccessGroups =>
        agentsExternalStubsConnector
          .massCreateAssistantsAndUsers(
            GranPermsGenRequest(idPrefix = "asa-test", numberOfAgents = 15, numberOfClients = 75)
          )
          .map(_ => None)

      case MmtarProvideDetails =>
        agentRegistrationConnector.testOnlyJourneySetup().map(linkId => Some(linkId.linkId))

    }

  private def getSignedInUser()(implicit rh: RequestHeader): Future[User] =
    agentsExternalStubsConnector.getUser(
      rh.session.get("userId").getOrElse(throw new RuntimeException("no userId in session"))
    )

  def createUser(
    affinityGroup: Option[AffinityGroup],
    services: List[EACDServiceKey],
    strideRole: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[User] = {

    val serviceKeys = services.map(_.key)

    val jsonUserBody = affinityGroup.fold(
      s"""{"strideRoles": ["${strideRole.get}"]}"""
    )(_ => s"""{"assignedPrincipalEnrolments":["${serviceKeys.mkString("\",\"")}"]}\"\"\"}\"\"\"}""")

    agentsExternalStubsConnector
      .createUser(
        affinityGroup = affinityGroup,
        Json.parse(jsonUserBody)
      )
      .flatMap(userCreated =>
        userCreated.assignedPrincipalEnrolments
          .find(_.service == "IR-SA")
          .map(_.identifiers.head.value)
          .map(utr => agentsExternalStubsConnector.updateUser(userCreated.copy(utr = Some(utr))))
          .getOrElse(Future.successful(()))
          .map(_ => userCreated)
      )
  }

  private def createAgentUser(
    serviceKey: Option[String] = Some("HMRC-AS-AGENT")
  )(implicit hc: HeaderCarrier): Future[User] =
    agentsExternalStubsConnector.createUser(
      affinityGroup = Some(Agent),
      userBody = serviceKey.fold[JsValue](JsNull)(sk =>
        Json.parse(
          s"""{"assignedPrincipalEnrolments":["$sk"]}"""
        )
      )
    )

  private def extractTestDataFromUser(user: User, asaJourneyService: ASAJourneyService)(implicit
    rh: RequestHeader
  ): Future[String] =
    asaJourneyService.customerKnownFact.fold(
      Future.successful(
        s"""{"${asaJourneyService.identifierName}": "${user.assignedPrincipalEnrolments.head.identifiers.head.value}"}"""
      )
    )(customerKnownFact =>
      if (asaJourneyService.identifierSourcedFromBpr) {
        agentsExternalStubsConnector.getRecord(user.recordIds.get.head).map { record =>
          val identifierValue = record.as[String](asaJourneyService.identifierReadsPath)

          val knownFact = s""" "${customerKnownFact.name}": "${record.as[String](customerKnownFact.reads)}"}"""

          s""" {"${asaJourneyService.identifierName}":"$identifierValue" $knownFact"""

        }
      } else {
        val userJson = Json.toJson(user)
        val identifierValue = easyCopy(userJson.as[String](asaJourneyService.identifierReadsPath))
        val mKnownFact: Option[String] =
          asaJourneyService.customerKnownFact.map(kf => s""" "${kf.name}": "${userJson.as[String](kf.reads)}"}""")

        Future.successful(s""" {"${asaJourneyService.identifierName}":"$identifierValue" ${mKnownFact
          .map(kf => s""", ${easyCopy(kf)}""")
          .getOrElse(s"""}""")}  """)
      }
    )

  private def easyCopy(str: String): String = str.replaceAll("\\s", "")

  private def extractArnFromAgent(user: User): String =
    user.assignedPrincipalEnrolments
      .find(_.service == "HMRC-AS-AGENT")
      .flatMap(_.identifiers.headOption.map(_.value))
      .getOrElse(throw new RuntimeException("identifier not found"))

  private def extractIdentifier(user: User, service: String) =
    user.assignedPrincipalEnrolments
      .find(_.service == service)
      .flatMap(_.identifiers.headOption.map(x => easyCopy(x.value)))
      .getOrElse(throw new RuntimeException("identifier not found"))

  private def extractOneIdentifier(user: User): String =
    user.assignedPrincipalEnrolments.headOption
      .flatMap(ek =>
        ek.service match {
          case "IR-SA" | "HMRC-MTD-IT" => user.nino.map(x => easyCopy(x.value))
          case _                       => ek.identifiers.headOption.map(x => easyCopy(x.value))
        }
      )
      .getOrElse(throw new RuntimeException("no identifier found"))

}
