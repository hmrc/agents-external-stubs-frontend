/*
 * Copyright 2022 HM Revenue & Customs
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

case class Group(
  planetId: String,
  groupId: String,
  affinityGroup: String,
  agentId: Option[String] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  suspendedRegimes: Set[String] = Set.empty
)

object Group {
  implicit val format: Format[Group] = Json.format[Group]
}

object AG {
  final val Individual = "Individual"
  final val Organisation = "Organisation"
  final val Agent = "Agent"

  val all: String => Boolean = Set(Individual, Organisation, Agent).contains
}
