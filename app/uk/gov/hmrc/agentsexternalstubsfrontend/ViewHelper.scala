package uk.gov.hmrc.agentsexternalstubsfrontend
import play.api.libs.json._

import scala.util.control.NonFatal

object ViewHelper {

  def intersperse[T](seq: Seq[T], e: T): Seq[T] =
    if (seq.size <= 1) seq
    else
      seq.init.flatMap(i => Seq(i, e)) :+ seq.last

  def excerpt(json: JsValue, paths: String*): JsValue =
    paths
      .map(p => p.split("\\."))
      .map(
        path =>
          (
            path,
            path.foldLeft[JsLookupResult](JsDefined(json))((j, p) =>
              j match {
                case JsDefined(arr: JsArray) =>
                  try {
                    arr(p.toInt)
                  } catch {
                    case NonFatal(_) => j
                  }
                case JsDefined(_) => j \ p
                case _            => j
            })))
      .map {
        case (path, JsDefined(value)) =>
          path.init.foldRight[JsValue](if (isArrayIndex(path.last)) Json.arr(value)
          else Json.obj(path.last -> value))((s, o) => if (isArrayIndex(s)) Json.arr(o) else Json.obj(s -> o))
        case _ => Json.obj()
      }
      .foldLeft[JsValue](Json.obj()) {
        case (o1: JsObject, o2: JsObject) => o1.deepMerge(o2)
        case (a1: JsArray, a2: JsArray)   => JsArray(a1.value ++ a2.value)
        case (x1, x2)                     => throw new UnsupportedOperationException(s"Could not merge $x1 and $x2")
      }

  private def isArrayIndex(s: String): Boolean = s == "*" || (try { s.toInt; true } catch { case _ => false })

}
