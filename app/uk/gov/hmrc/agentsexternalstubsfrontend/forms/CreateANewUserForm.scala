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

import play.api.data.{Form, Mapping}
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

case class CreateANewUser(
  userId: Option[String]
)

object CreateANewUserForm {

  private def createANewUserId: Mapping[Option[String]] = optional(text verifying createANewUserIdConstraint)

  private val userIdRegex = "^[A-Za-z0-9-_]{3,64}"

  private val createANewUserIdConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    fieldValue match {
      case value if !value.matches(userIdRegex) =>
        Invalid(ValidationError("error.userId.invalid"))
      case _ => Valid
    }
  }

  val form: Form[CreateANewUser] =
    Form[CreateANewUser](
      mapping(
        "userId" -> createANewUserId
      )(CreateANewUser.apply)(CreateANewUser.unapply)
    )

}
