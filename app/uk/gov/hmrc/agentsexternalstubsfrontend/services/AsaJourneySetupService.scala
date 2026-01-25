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
import play.api.libs.json.{JsNull, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.{AgentClientRelationshipsConnector, AgentRegistrationConnector, AgentsExternalStubsConnector}
import uk.gov.hmrc.agentsexternalstubsfrontend.controllers.routes
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.SignInRequest
import uk.gov.hmrc.agentsexternalstubsfrontend.models.AsaTestJourney._
import uk.gov.hmrc.agentsexternalstubsfrontend.models._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.agentsexternalstubsfrontend.util.RequestSupport._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AsaJourneySetupService @Inject() (
  agentsExternalStubsConnector: AgentsExternalStubsConnector,
  agentRegistrationConnector: AgentRegistrationConnector,
  acrConnector: AgentClientRelationshipsConnector,
  frontendConfig: FrontendConfig
)(implicit ec: ExecutionContext) {

  private def withActiveSession(
    f: (String) => Future[JourneySetup]
  )(implicit hc: HeaderCarrier): Future[JourneySetup] =
    agentsExternalStubsConnector.signIn().flatMap { authSession =>
      agentsExternalStubsConnector
        .removeUser(authSession.userId)
        .flatMap(_ => f(authSession.planetId))
    }

  def setupJourney(journey: String)(implicit hc: HeaderCarrier): Future[JourneySetup] =
    journey match {
      case "provide-details" =>
        withActiveSession { (planetId: String) =>
          val services = List(Itsa)
          for {
            ind    <- createUser(JourneySetupUser(Individual, services))(hc)
            signIn <- agentsExternalStubsConnector.signIn(SignInRequest(ind.userId, planetId))
            linkId <- agentRegistrationConnector.testOnlyJourneySetup()(hc)
          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl =
              s"${frontendConfig.agentRegistrationFrontendExternalUrl}/agent-registration/provide-details/start/${linkId.linkId}",
            journeyData = None,
            journey = "provide-details",
            service = None
          )
        }
      case "myta-org" =>
        withActiveSession { (planetId: String) =>
          val services = List(Vat, Cbc, Ppt, Trust, TrustNT, CapitalGains)
          for {
            org <- createUser(
                     JourneySetupUser(
                       affinityGroup = Organisation,
                       services = services
                     )
                   )(hc)
            agent <- createAgentUser()
            arn = extractArnFromAgent(agent)
            invitations = services.map(svc =>
                            JourneySetupInvitation(
                              arn,
                              extractIdentifier(org, svc.client.principalServiceKey.get),
                              svc.relationship.clientIdTypeForRel,
                              org.name.getOrElse(""),
                              svc.relationship.delegatedServiceKey,
                              Some("business")
                            )
                          )
            signIn <- agentsExternalStubsConnector.signIn(SignInRequest(org.userId, planetId))
            _ <- acrConnector.testOnlyJourneySetup(
                   JourneySetupRequest(invitations = invitations)
                 )

          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl = s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/myta",
            journeyData = None,
            journey = "myta-org",
            service = None
          )
        }

      case "myta-ind" =>
        withActiveSession { (planetId: String) =>
          for {
            ind <- createUser(
                     JourneySetupUser(
                       affinityGroup = Individual,
                       List(Itsa)
                     )
                   )(hc)
            nino = ind.nino.get.value
            agent1 <- createAgentUser()(hc)
            arn1 = extractArnFromAgent(agent1)
            agent2 <- createAgentUser()(hc)
            arn2 = extractArnFromAgent(agent2)
            signIn <- agentsExternalStubsConnector.signIn(SignInRequest(ind.userId, planetId))
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

          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl = s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/myta",
            journeyData = None,
            journey = "myta-ind",
            service = None
          )
        }

      case "access-groups" =>
        withActiveSession { (planetId: String) =>
          for {
            agent  <- createAgentUser()
            signIn <- agentsExternalStubsConnector.signIn(SignInRequest(agent.userId, planetId))
            _ <- agentsExternalStubsConnector.massCreateAssistantsAndUsers(
                   GranPermsGenRequest(idPrefix = "asa-test", numberOfAgents = 15, numberOfClients = 75)
                 ) //(hc.copy(authorization = Some(Authorization(s"Bearer ${signIn.authToken}"))), ec)

          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl =
              s"${frontendConfig.asafHost}/agent-services-account/manage-account", //could replace with a test-only url and let the service decide where to go.
            journeyData = None,
            journey = "access-groups",
            service = None
          )
        }

      case serviceLed @ ("create-invitation" | "agent-led-deauth" | "hmrc-led-deauth") =>
        Future.successful(
          JourneySetup(None, routes.AsaJourneySetupController.showSelectService.url, None, serviceLed, None)
        )

      case "track" =>
        withActiveSession { (planetId: String) =>
          createAgentUser()
            .flatMap(agent =>
              Future
                .sequence(
                  supportedServices.map(service =>
                    createUser(
                      JourneySetupUser(
                        affinityGroup = service.client.affinityGroup,
                        services = List(service)
                      )
                    ).map(user =>
                      JourneySetupInvitation(
                        extractArnFromAgent(agent),
                        extractOneIdentifier(user),
                        service.relationship.clientIdTypeForRel,
                        user.name.getOrElse("bob"),
                        service.relationship.delegatedServiceKey,
                        None
                      )
                    )
                  )
                )
                .flatMap(invitations =>
                  for {
                    signin <- agentsExternalStubsConnector.signIn(SignInRequest(agent.userId, planetId))
                    _      <- acrConnector.testOnlyJourneySetup(JourneySetupRequest(invitations = invitations))
                  } yield JourneySetup(
                    authSession = Some(signin),
                    redirectUrl =
                      s"${frontendConfig.acrfHost}/agent-client-relationships/test-only/journey-setup/track",
                    journeyData = None,
                    journey = "track",
                    service = None
                  )
                )
            )
        }
    }

  def setupDataForJourneyWithServiceSelected(journey: JourneyWithServiceSelected, asaService: AsaService)(implicit
    rh: RequestHeader
  ): Future[String] =
    journey match {
      case CreateInvitation =>
        for {
          customer <- createUser(
                        JourneySetupUser(
                          affinityGroup = asaService.client.affinityGroup,
                          services = List(asaService)
                        )
                      )
          testData <- extractTestDataFromUser(customer, asaService)
        } yield testData

      case AgentLedDeauth =>
        for {
          customer <- createUser(
                        JourneySetupUser(
                          affinityGroup = asaService.client.affinityGroup,
                          services = List(asaService)
                        )
                      )
          agent <- agentsExternalStubsConnector.getUser(
                     rh.session.get("userId").getOrElse(throw new RuntimeException("no userId in session"))
                   )
          arn = extractArnFromAgent(agent)
          clientId = extractOneIdentifier(customer)
          _ <- acrConnector.testOnlyCreateRelationship(
                 arn,
                 clientId,
                 asaService.relationship.delegatedServiceKey,
                 asaService.relationship.clientIdTypeForRel
               )
          testData <- extractTestDataFromUser(customer, asaService)
        } yield testData

      case HmrcLedDeauth =>
        for {
          customer <- createUser(
                        JourneySetupUser(
                          affinityGroup = asaService.client.affinityGroup,
                          services = List(asaService)
                        )
                      )
          agent <- createAgentUser()
          arn = extractArnFromAgent(agent)
          clientId = extractOneIdentifier(customer)
          _ <- acrConnector.testOnlyCreateRelationship(
                 arn,
                 clientId,
                 asaService.relationship.delegatedServiceKey,
                 asaService.relationship.clientIdTypeForRel
               )
          testData <- extractTestDataFromUser(customer, asaService)
        } yield testData
    }

  def setupDataForJourneyWithoutServiceSelected(
    journey: JourneyWithoutServiceSelected
  )(implicit rh: RequestHeader): Future[Option[String]] =
    journey match {
      case MytaInd =>
        for {
          ind <- agentsExternalStubsConnector.getUser(
                   rh.session.get("userId").getOrElse(throw new RuntimeException("no userId in session"))
                 )
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
          org <- agentsExternalStubsConnector.getUser(
                   rh.session.get("userId").getOrElse(throw new RuntimeException("no userId in session"))
                 )
          agent <- createAgentUser()
          arn = extractArnFromAgent(agent)
          invitations = journey.signedInUserServices.map { service =>
                          JourneySetupInvitation(
                            arn,
                            extractIdentifier(org, service.client.principalServiceKey.get),
                            service.client.identifierName,
                            org.name.getOrElse(""),
                            service.relationship.delegatedServiceKey,
                            Some("business")
                          )
                        }
          _ <- acrConnector.testOnlyJourneySetup(
                 JourneySetupRequest(invitations = invitations)
               )
        } yield None
      case Track               => ???
      case AccessGroups        => ???
      case MmtarProvideDetails => ???
      case UkSubscription      => ???

    }

  def setupJourneyForServiceSelected(journey: String, asaService: AsaService)(implicit
    rh: RequestHeader
  ): Future[JourneySetup] =
    journey match {
      case "create-invitation" =>
        withActiveSession { (planetId: String) =>
          for {
            customer <- createUser(
                          JourneySetupUser(
                            affinityGroup = asaService.client.affinityGroup,
                            services = List(asaService)
                          )
                        )
            agent    <- createAgentUser()
            testData <- extractTestDataFromUser(customer, asaService)
            signIn <-
              agentsExternalStubsConnector.signIn(SignInRequest(agent.userId, planetId))
            service = s"${asaService.id}"
          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl = s"${routes.AsaJourneySetupController.showTestData.url}",
            journeyData = Some(testData),
            journey = journey,
            service = Some(service)
          )
        }

      case "agent-led-deauth" =>
        withActiveSession { (planetId: String) =>
          for {
            customer <- createUser(
                          JourneySetupUser(
                            affinityGroup = asaService.client.affinityGroup,
                            services = List(asaService)
                          )
                        )(hc)
            agent <- createAgentUser()(hc)
            arn = extractArnFromAgent(agent)
            clientId = extractOneIdentifier(customer)
            testData <- extractTestDataFromUser(customer, asaService)
            signIn <-
              agentsExternalStubsConnector.signIn(SignInRequest(agent.userId, planetId))
            _ <- acrConnector.testOnlyCreateRelationship(
                   arn,
                   clientId,
                   asaService.relationship.delegatedServiceKey,
                   asaService.relationship.clientIdTypeForRel
                 )(hc.copy(authorization = Some(Authorization(s"Bearer ${signIn.authToken}"))))

            service = s"${asaService.id}"
          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl = s"${routes.AsaJourneySetupController.showTestData.url}",
            journeyData = Some(testData),
            journey = journey,
            service = Some(service)
          )
        }

      case "hmrc-led-deauth" =>
        withActiveSession { (planetId: String) =>
          for {
            customer <- createUser(
                          JourneySetupUser(
                            affinityGroup = asaService.client.affinityGroup,
                            services = List(asaService)
                          )
                        )(hc)
            agent <- createAgentUser()
            arn = extractArnFromAgent(agent)
            clientId = extractOneIdentifier(customer)
            testData <- extractTestDataFromUser(customer, asaService)
            _        <- createStrideUser(customer.name.getOrElse(""), "maintain_agent_relationships")
            _ <- acrConnector.testOnlyCreateRelationship(
                   arn,
                   clientId,
                   asaService.relationship.delegatedServiceKey,
                   asaService.relationship.clientIdTypeForRel
                 )
            signIn <-
              agentsExternalStubsConnector.signIn(
                SignInRequest(customer.name.getOrElse(""), planetId, providerType = AuthProvider.PrivilegedApplication)
              )(hc, ec)
            service = s"${asaService.id}"
          } yield JourneySetup(
            authSession = Some(signIn),
            redirectUrl = s"${routes.AsaJourneySetupController.showTestData.url}",
            journeyData = Some(testData),
            journey = journey,
            service = Some(service)
          )
        }
    }

  private def createUser(user: JourneySetupUser)(implicit hc: HeaderCarrier): Future[User] =
    agentsExternalStubsConnector
      .createUser(
        affinityGroup = Some(user.affinityGroup),
        if (user.services.isEmpty) JsNull
        else
          Json.parse(
            s"""{"assignedPrincipalEnrolments":["${serviceKeys(user.services).mkString("\",\"")}"]}"""
          )
      )
      .flatMap(userCreated =>
        userCreated.assignedPrincipalEnrolments
          .find(_.service == "IR-SA")
          .map(_.identifiers.head.value)
          .map(utr => agentsExternalStubsConnector.updateUser(userCreated.copy(utr = Some(utr))))
          .getOrElse(Future.successful(()))
          .map(_ => userCreated)
      )

  private def serviceKeys(services: List[AsaService]): List[String] =
    services.flatMap(_.client.principalServiceKey)

  private def createStrideUser(userId: String, role: String)(implicit hc: HeaderCarrier): Future[Unit] =
    agentsExternalStubsConnector.createUser(
      user = User(userId = userId, strideRoles = Seq(role)),
      affinityGroup = None
    )

  private def createAgentUser(serviceKey: String = "HMRC-AS-AGENT")(implicit hc: HeaderCarrier): Future[User] =
    agentsExternalStubsConnector.createUser(
      affinityGroup = Some(Agent),
      userBody = Json.parse(
        s"""{"assignedPrincipalEnrolments":["$serviceKey"]}"""
      )
    )

  private def extractTestDataFromUser(user: User, service: AsaService)(implicit rh: RequestHeader): Future[String] =
    if (AsaTestJourney.isTrustService(service))
      Future.successful(
        s"""{"${service.client.identifierName}": "${user.assignedPrincipalEnrolments.head.identifiers.head.value}"}"""
      )
    else {
      if (service.client.identifierSourcedFromBpr) {
        agentsExternalStubsConnector.getRecord(user.recordIds.get.head).map { record =>
          val identifierValue = record.as[String](service.client.identifierReadsPath)

          val mKnownFact = service.knownFact.map(kf => s""" "${kf.name}": "${record.as[String](kf.reads)}"}""")

          s""" {"${service.client.identifierName}":"$identifierValue" ${mKnownFact
            .map(kf => s""", ${easyCopy(kf)}""")
            .getOrElse(s"""}""")}  """

        }

      } else {
        val userJson = Json.toJson(user)
        val identifierValue = easyCopy(userJson.as[String](service.client.identifierReadsPath))
        val mKnownFact: Option[String] =
          service.knownFact.map(kf => s""" "${kf.name}": "${userJson.as[String](kf.reads)}"}""")

        Future.successful(s""" {"${service.client.identifierName}":"$identifierValue" ${mKnownFact
          .map(kf => s""", ${easyCopy(kf)}""")
          .getOrElse(s"""}""")}  """)
      }
    }

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
