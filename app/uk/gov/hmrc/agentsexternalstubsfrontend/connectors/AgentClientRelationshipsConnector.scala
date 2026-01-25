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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubsfrontend.config.FrontendConfig
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{JourneySetupRequest, User}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientRelationshipsConnector @Inject() (appConfig: FrontendConfig, http: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  def testOnlyJourneySetup(journeySetupRequest: JourneySetupRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.acrBaseUrl}/test-only/journey-setup"
    http.put(url).withBody(Json.toJson(journeySetupRequest)).execute[HttpResponse].map(_ => ())
  }

  def testOnlyCreateRelationship(arn: String, clientId: String, service: String, clientIdType: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val url = url"${appConfig.acrBaseUrl}/test-only/agent/$arn/service/$service/client/$clientIdType/$clientId"
    http.put(url).execute[HttpResponse].map(_ => ())
  }
}
