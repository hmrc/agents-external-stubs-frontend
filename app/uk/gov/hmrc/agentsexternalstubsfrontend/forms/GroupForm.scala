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

package uk.gov.hmrc.agentsexternalstubsfrontend.forms

import play.api.data.Forms.{ignored, mapping, nonEmptyText, optional, seq, text}
import play.api.data.Form
import uk.gov.hmrc.agentsexternalstubsfrontend.forms.ValidateHelper.enrolmentMapping
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{AffinityGroup, Enrolment, Group}

object GroupForm {

  val form: Form[Group] = Form[Group](
    mapping(
      "planetId"          -> ignored("ignored"),
      "groupId"           -> ignored("ignored"),
      "affinityGroup"     -> text.verifying(ag => AffinityGroup.definedValues.contains(ag)),
      "agentId"           -> optional(nonEmptyText),
      "agentCode"         -> optional(nonEmptyText),
      "agentFriendlyName" -> optional(nonEmptyText),
      "principalEnrolments" -> seq(enrolmentMapping)
        .transform[Seq[Enrolment]](_.collect { case Some(e) => e }, _.map(Some(_))),
      "delegatedEnrolments" -> seq(enrolmentMapping)
        .transform[Seq[Enrolment]](_.collect { case Some(e) => e }, _.map(Some(_))),
      "suspendedRegimes" -> optional(seq(optional(nonEmptyText))).transform[Set[String]](
        _.getOrElse(Seq.empty).collect { case Some(x) => x }.toSet,
        set => Some(set.toSeq.map(Some(_)))
      )
    )(Group.apply)(Group.unapply)
  )

}
