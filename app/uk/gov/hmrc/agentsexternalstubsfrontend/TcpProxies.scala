/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Tcp}
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TcpProxies @Inject (
  appConfig: FrontendConfig,
  applicationLifecycle: ApplicationLifecycle
)(using system: ActorSystem)
    extends Logging:

  val proxiedServices: Map[String, Int] = Map(
    "company-auth-frontend"                    -> appConfig.companyAuthFEPort,
    "bas-gateway-frontend"                     -> appConfig.basGatewayFEPort,
    "stride-auth-frontend"                     -> appConfig.strideAuthFEPort,
    "identity-verification-frontend"           -> appConfig.ivFEPort,
    "government-gateway-registration-frontend" -> appConfig.ggRegEPort,
    "personal-details-validation-frontend"     -> appConfig.pvDetailsValidationFEPort,
    "auth-login-stub"                          -> appConfig.authLoginStubPort
  )

  if !appConfig.proxiesStart then logger.info("TCP proxying feature is switched off")
  else
    logger.info("Starting local TCP proxies ...")

    given ExecutionContext = system.dispatcher

    val tcp = Tcp()
    val tcpProxy = Flow[ByteString].via(tcp.outgoingConnection("localhost", appConfig.tcpProxyPort))

    def startProxy(serviceName: String, port: Int): Future[Unit] =
      tcp
        .bindAndHandle(tcpProxy, interface = "localhost", port = port)
        .map: binding =>
          logger.info(s"Listening for $serviceName requests on ${binding.localAddress}")
          applicationLifecycle.addStopHook: () =>
            logger.info(s"Stopping TCP proxy for $serviceName requests on $port")
            binding.unbind()
        .recover: err =>
          logger.error(s"Could not start TCP proxy for $serviceName requests on $port because of $err")

    Future.traverse(proxiedServices)(startProxy)
