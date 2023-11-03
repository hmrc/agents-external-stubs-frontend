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

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.libs.json.{JsObject, Json}

import scala.util.control.NonFatal

object RecordForm {

  val validJson: Constraint[String] = Constraint(json =>
    try { Json.parse(json); Valid }
    catch { case NonFatal(e) => Invalid(e.getMessage) }
  )

  val form: Form[JsObject] =
    Form[JsObject](
      mapping("json" -> text.verifying(validJson))(t => Json.parse(t).as[JsObject])(j => Some(Json.prettyPrint(j)))
    )

}
