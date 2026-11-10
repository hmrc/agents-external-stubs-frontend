/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubsfrontend.support

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import java.net.ServerSocket
import scala.util.Using

trait TestPlayServer extends GuiceOneServerPerSuite:
  suite: TestSuite =>

  /** Choosing a port for the test server rather than letting it pick one, since we need to know the port in advance
    * (i.e. in the configuration) for the port-forwarding setup.
    */
  private val assignedPort: Int =
    sys.props
      .getOrElseUpdate(
        "testserver.port",
        s"${Using.resource(new ServerSocket(0))(serverSocket => serverSocket.getLocalPort)}"
      )
      .toInt

  val wireMockPort: Int = Using.resource(new ServerSocket(0))(serverSocket => serverSocket.getLocalPort)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.agents-external-stubs.port"      -> wireMockPort,
        "microservice.services.auth.port"                       -> wireMockPort,
        "microservice.services.agent-registration.port"         -> wireMockPort,
        "microservice.services.agent-client-relationships.port" -> wireMockPort,
        "metrics.enabled"                                   -> false,
        "auditing.enabled"                                  -> false,
        "tcpProxyTargetPort"                                         -> assignedPort,
      )
      .build()
