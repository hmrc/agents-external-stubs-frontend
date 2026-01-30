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

package uk.gov.hmrc.agentsexternalstubsfrontend.support

import uk.gov.hmrc.agentsexternalstubsfrontend.models.{EnrolmentKey, Identifier, User}
import uk.gov.hmrc.domain.Nino

object TestFixtures {

  val arn = "HARN0001234"
  val arn2 = "HARN0001235"
  val nino = "OZ371091C"
  val mtdItId = "OZHF16885442887"

  val agentRefNo = "585377395"


  val asaEnrolment = EnrolmentKey(service = "HMRC-AS-AGENT", identifiers = Seq(Identifier("agentReferenceNumber",s"$arn")))
  val asaEnrolment2 = EnrolmentKey(service = "HMRC-AS-AGENT", identifiers = Seq(Identifier("agentReferenceNumber",s"$arn2")))
  val itsaEnrolment = EnrolmentKey(service = "HMRC-MTD-IT", identifiers = Seq(Identifier("MDITID",s"$mtdItId")))
  val ptEnrolment = EnrolmentKey(service = "HMRC-PT", identifiers = Seq(Identifier("NINO", s"$nino")))
  val hmceVatAgntEnrolment = EnrolmentKey(service = "HMCE-VAT-AGNT", identifiers = Seq(Identifier("AgentRefNo", s"$agentRefNo")))

  def agentOrClientUser(userId: String, enrolments: List[EnrolmentKey]) = User(userId = userId, nino = Some(Nino(nino)),
    assignedPrincipalEnrolments = enrolments, recordIds = Some(Seq("1234")))


}
