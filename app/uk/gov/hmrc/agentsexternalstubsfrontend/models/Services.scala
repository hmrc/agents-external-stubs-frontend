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

package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json.{Format, Json}

case class Service(
  name: String,
  description: String,
  affinityGroups: Seq[String],
  identifiers: Seq[Service.Identifier],
  knownFacts: Seq[Service.KnownFact],
  flags: Service.Flags) {

  def getIdentifier(name: String): Option[Service.Identifier] = identifiers.find(_.name.toUpperCase == name.toUpperCase)

  def getKnownFact(name: String): Option[Service.KnownFact] = knownFacts.find(_.name.toUpperCase == name.toUpperCase)
}

object Service {

  case class Identifier(name: String, description: String, regex: String)

  case class KnownFact(name: String, description: String, regex: String)

  case class Flags(
    uniqueIdentifiers: Boolean,
    agentExclusive: Boolean,
    agentExcludesPrincipal: Boolean,
    multipleEnrolment: Boolean,
    autoEnrolment: Boolean,
    autoActivation: Boolean)
}

case class Services(services: Seq[Service]) {

  def getService(name: String): Option[Service] = services.find(_.name == name)

  def options: Seq[(String, String)] = services.map(s => (s.name, s.affinityGroups.mkString(" "))).sortBy(_._1)

  def servicesFor(affinityGoup: String): Seq[Service] = services.collect {
    case s if s.affinityGroups.contains(affinityGoup) => s
  }
}

object Services {

  implicit val f0: Format[Service.Identifier] = Json.format[Service.Identifier]
  implicit val f1: Format[Service.KnownFact] = Json.format[Service.KnownFact]
  implicit val f2: Format[Service.Flags] = Json.format[Service.Flags]
  implicit val f3: Format[Service] = Json.format[Service]
  implicit val f4: Format[Services] = Json.format[Services]
}
