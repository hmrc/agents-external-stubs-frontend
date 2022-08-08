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

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/** Request for generating large number of users for the purpose of testing Granular Permissions.
  */
case class GranPermsGenRequest(
  idPrefix: String,
  numberOfAgents: Int,
  numberOfClients: Int,
  fillFriendlyNames: Boolean = false,
  clientTypeDistribution: Option[Map[String, Double]] = None,
  individualServiceDistribution: Option[Map[String, Double]] = None,
  organisationServiceDistribution: Option[Map[String, Double]] = None
)

object GranPermsGenRequest {
  implicit val format = Json.format[GranPermsGenRequest]
}

case class GranPermsGenResponse(
  createdAgents: Seq[User],
  createdClients: Seq[User]
)

object GranPermsGenResponse {
  implicit val format = Json.format[GranPermsGenResponse]
}

object GranPermsGenRequestForm {
  val form: Form[GranPermsGenRequest] = Form[GranPermsGenRequest](
    mapping(
      "idPrefix"          -> nonEmptyText,
      "numberOfAgents"    -> number,
      "numberOfClients"   -> number,
      "fillFriendlyNames" -> optional(boolean).transform[Boolean](_.isDefined, x => if (x) Some(x) else None)
    )(formApply)(formUnapply)
  )

  private def formApply(idPrefix: String, nAgents: Int, nClients: Int, fillFriendly: Boolean) =
    GranPermsGenRequest(
      idPrefix = idPrefix,
      numberOfAgents = nAgents,
      numberOfClients = nClients,
      fillFriendlyNames = fillFriendly
    )

  private def formUnapply(genRequest: GranPermsGenRequest): Option[(String, Int, Int, Boolean)] =
    Some((genRequest.idPrefix, genRequest.numberOfAgents, genRequest.numberOfClients, genRequest.fillFriendlyNames))
}
