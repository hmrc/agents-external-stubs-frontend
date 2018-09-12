package uk.gov.hmrc.agentsexternalstubsfrontend

object ViewHelper {

  def intersperse[T](seq: Seq[T], e: T): Seq[T] =
    seq.init.flatMap(i => Seq(i, e)) :+ seq.last

}
