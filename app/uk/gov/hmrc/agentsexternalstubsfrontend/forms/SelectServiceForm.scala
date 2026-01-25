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

package uk.gov.hmrc.agentsexternalstubsfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{single, text}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.{AsaService, AsaTestJourney}

object SelectServiceForm {

  val servicesForCreateInvitationJourney: Seq[AsaService] = AsaTestJourney.createInvitationServices
  val servicesForDeauthorisation: Seq[AsaService] = AsaTestJourney.supportedServices

  def servicesForJourney(journey: String): Seq[AsaService] =
    if (journey == "create-invitation") servicesForCreateInvitationJourney else servicesForDeauthorisation

  def selectServiceFormForJourney(journey: String): Form[AsaService] = Form(
    single(
      "service" -> text
        .verifying("select-service.error", x => servicesForJourney(journey).map(_.id).contains(x))
        .transform(str => AsaTestJourney.forId(str), (svc: AsaService) => AsaTestJourney.unapply(svc).get)
    )
  )
}
