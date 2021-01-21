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

package uk.gov.hmrc.agentsexternalstubsfrontend.config

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendConfig @Inject() (servicesConfig: ServicesConfig) {

  val appName = "agents-external-stubs-frontend"

  private def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)

  private def getConfInt(config: String) =
    servicesConfig.getConfInt(config, throw new RuntimeException(s"config $config not found"))

  val companyAuthFEPort = getConfInt("company-auth-frontend.port")

  val basGatewayFEPort = getConfInt("bas-gateway-frontend.port")

  val strideAuthFEPort = getConfInt("stride-auth-frontend.port")

  val ivFEPort = getConfInt("identity-verification-frontend.port")

  val ggRegEPort = getConfInt("government-gateway-registration-frontend.port")

  val pvDetailsValidationFEPort = getConfInt("personal-details-validation-frontend.port")

  val httpPort = servicesConfig.getInt("http.port")

  val proxiesStart = servicesConfig.getBoolean("proxies.start")

  val authBaseUrl = baseUrl("auth")

  val aesBaseUrl = baseUrl("agents-external-stubs")
}
