/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TcpProxies @Inject() (appConfig: FrontendConfig)(implicit system: ActorSystem, materializer: Materializer) {

  private val startProxies = appConfig.proxiesStart
  private val companyAuthFrontendPort = appConfig.companyAuthFEPort
  private val basGatewayFrontendPort = appConfig.basGatewayFEPort
  private val strideAuthFrontendPort = appConfig.strideAuthFEPort
  private val identityVerificationFrontendPort = appConfig.ivFEPort
  private val governmentGatewayRegistrationFrontendPort = appConfig.ggRegEPort
  private val personalDetailsValidationFrontendPort = appConfig.pvDetailsValidationFEPort
  private val authLoginStubPort = appConfig.authLoginStubPort
  private val httpPort = appConfig.httpPort

  if (startProxies) {
    println("Starting TCP proxies ...")

    implicit val ec: ExecutionContext = system.dispatcher

    val agentsExternalStubsFrontendPort = Try(httpPort.toInt).toOption.getOrElse(9009)

    val tcpOutgoingConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
      Tcp().outgoingConnection("localhost", agentsExternalStubsFrontendPort)

    val tcpProxy = Flow[ByteString].via(tcpOutgoingConnection)

    def startProxy(port: Int, serviceName: String): Future[Unit] =
      Tcp(system)
        .bindAndHandle(tcpProxy, interface = "localhost", port = port)
        .map(s => Logger(getClass).info(s"Listening for $serviceName requests on ${s.localAddress}"))
        .recover { case e: Exception =>
          Logger(getClass).error(s"Could not start TCP proxy for $serviceName requests on $port because of $e")
        }

    startProxy(companyAuthFrontendPort, "company-auth-frontend")
    startProxy(basGatewayFrontendPort, "bas-gateway-frontend")
    startProxy(strideAuthFrontendPort, "stride-auth-frontend")
    startProxy(identityVerificationFrontendPort, "identity-verification-frontend")
    startProxy(governmentGatewayRegistrationFrontendPort, "government-gateway-registration-frontend")
    startProxy(personalDetailsValidationFrontendPort, "personal-details-validation-frontend")
    startProxy(authLoginStubPort, "auth-login-stub")

  } else {
    println("TCP proxies feature switched off")
  }

}
