package uk.gov.hmrc.agentsexternalstubsfrontend.connectors
import uk.gov.hmrc.http.{HttpException, Upstream4xxResponse}

class Upstream4xxException(msg: String, code: Int) extends HttpException(msg, code)

object Upstream4xxException {
  def unapply(e: Exception): Option[Upstream4xxException] = {
    val (code, msg) = e match {
      case e: HttpException =>
        (e.responseCode, {
          val m = e.getMessage
          val p = ",\"message\":\""
          val s = m.indexOf(p) + p.length
          if (s > 0) m.substring(s, m.length - 3) else m
        })
      case e: Upstream4xxResponse =>
        (e.upstreamResponseCode, {
          val m = e.getMessage
          val p = "Response body: '"
          val s = m.indexOf(p) + p.length
          if (s > 0) m.substring(s, m.length - 1) else m
        })
      case _ => (0, "")
    }
    if (code >= 400 && code < 500) {
      Some(new Upstream4xxException(msg, code))
    } else None
  }
}
