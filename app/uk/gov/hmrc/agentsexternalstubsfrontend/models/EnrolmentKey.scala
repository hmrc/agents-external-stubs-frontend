package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.libs.json._

case class EnrolmentKey(service: String, identifiers: Seq[Identifier]) {
  def isSingle: Boolean = identifiers.size == 1
  lazy val tag = s"$service~${identifiers.sorted.mkString("~")}"
}

object EnrolmentKey {

  def apply(s: String): Either[String, EnrolmentKey] = {
    val parts = s.split("~")
    if (parts.nonEmpty && parts.size >= 3 && parts.size % 2 == 1) {
      val service = parts.head
      val identifiers = parts.tail.sliding(2, 2).map(a => Identifier(a(0), a(1))).toSeq
      Right(EnrolmentKey(service, identifiers))
    } else Left("INVALID_ENROLMENT_KEY")
  }

  implicit val reads: Reads[EnrolmentKey] = new Reads[EnrolmentKey] {
    override def reads(json: JsValue): JsResult[EnrolmentKey] = json match {
      case JsString(value) => apply(value).fold(JsError.apply, JsSuccess.apply(_))
      case _               => JsError("STRING_VALUE_EXPECTED")
    }
  }

}
