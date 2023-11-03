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

import play.api.data.Forms.{mapping, nonEmptyText, number, optional, seq, text}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.agentsexternalstubsfrontend.models.SpecialCase

object SpecialCaseForm {

  val requestMatchMapping: Mapping[SpecialCase.RequestMatch] = mapping(
    "path" -> nonEmptyText
      .transform[String](path => if (!path.startsWith("/")) "/" + path else path, identity)
      .verifying(path => path != "/")
      .verifying(path => !(path.startsWith("/agents-external-stubs") || path.startsWith("/ping"))),
    "method"      -> nonEmptyText,
    "body"        -> optional(nonEmptyText),
    "contentType" -> optional(nonEmptyText)
  )(SpecialCase.RequestMatch.apply)(SpecialCase.RequestMatch.unapply)

  val headerMapping: Mapping[SpecialCase.Header] = mapping(
    "name"  -> nonEmptyText,
    "value" -> nonEmptyText
  )(SpecialCase.Header.apply)(SpecialCase.Header.unapply)

  val responseMapping: Mapping[SpecialCase.Response] = mapping(
    "status" -> number(200, 599),
    "body"   -> optional(nonEmptyText),
    "headers" -> optional(seq(optional(headerMapping)))
      .transform[Option[Seq[SpecialCase.Header]]](_.map(_.collect { case Some(x) => x }), _.map(_.map(Option.apply)))
  )(SpecialCase.Response.apply)(SpecialCase.Response.unapply)

  val form: Form[SpecialCase] = Form[SpecialCase](
    mapping(
      "requestMatch" -> requestMatchMapping,
      "response"     -> responseMapping,
      "id"           -> optional(text)
    )(SpecialCase.apply)(SpecialCase.unapply)
  )

}
