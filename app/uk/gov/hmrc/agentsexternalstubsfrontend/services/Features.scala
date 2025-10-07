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

package uk.gov.hmrc.agentsexternalstubsfrontend.services
import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Features @Inject() (configuration: Configuration) {

  def mayShowRestQuery(planetId: String): Boolean =
    showRestQuery || allowlistedPlanetPrefix.exists(planetId.startsWith)

  private lazy val showRestQuery: Boolean = configuration
    .getOptional[Boolean]("features.show-rest-query")
    .getOrElse(true)

  lazy val showEnrolments: Boolean = configuration
    .getOptional[Boolean]("features.show-enrolments")
    .getOrElse(true)

  private lazy val allowlistedPlanetPrefix: Option[String] = configuration
    .getOptional[String]("allowlisted-planet-prefix")
}
