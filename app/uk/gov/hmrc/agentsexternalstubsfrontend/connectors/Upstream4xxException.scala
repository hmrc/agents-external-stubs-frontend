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

package uk.gov.hmrc.agentsexternalstubsfrontend.connectors
import uk.gov.hmrc.http.{HttpException, Upstream4xxResponse}

class Upstream4xxException(msg: String, code: Int) extends HttpException(msg, code)

object Upstream4xxException {

  def unapply(e: Exception): Option[Upstream4xxException] = {
    val (code, msg) = e match {
      case e: HttpException =>
        (
          e.responseCode, {
            val m = e.getMessage
            val p = ",\"message\":\""
            val s = m.indexOf(p) + p.length
            if (s > 0) m.substring(s, m.length - 3) else m
          }
        )
      case e: Upstream4xxResponse =>
        (
          e.upstreamResponseCode, {
            val m = e.getMessage
            val p = "Response body: '"
            val s = m.indexOf(p) + p.length
            if (s > 0) m.substring(s, m.length - 1) else m
          }
        )
      case _ => (0, "")
    }
    if (code >= 400 && code < 500) {
      Some(new Upstream4xxException(msg, code))
    } else None
  }
}
