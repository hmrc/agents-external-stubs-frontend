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
        case (o1: JsObject, o2: JsObject) => deepMerge(o1, o2)
        case (a1: JsArray, a2: JsArray)   => JsArray(a1.value ++ a2.value)
        case (x1, x2)                     => throw new UnsupportedOperationException(s"Could not merge $x1 and $x2")
      }

  private def isArrayIndex(s: String): Boolean = s == "*" || (try { s.toInt; true } catch { case NonFatal(_) => false })

  private def deepMerge(o1: JsObject, o2: JsObject): JsObject = {
    val result = o1.value ++ o2.value.map {
      case (otherKey, otherValue) =>
        val maybeExistingValue = o1.value.get(otherKey)

        val newValue = (maybeExistingValue, otherValue) match {
          case (Some(e: JsObject), o: JsObject) => deepMerge(e, o)
          case (Some(e: JsArray), a: JsArray)   => deepMerge(e, a)
          case _                                => otherValue
        }
        otherKey -> newValue
    }
    JsObject(result)
  }

  private def deepMerge(a1: JsArray, a2: JsArray): JsArray =
    JsArray(a1.value.zipAll(a2.value, JsNull, JsNull).map {
      case (i1: JsObject, i2: JsObject) => deepMerge(i1, i2)
      case (i1: JsArray, i2: JsArray)   => deepMerge(i1, i2)
      case (i1, JsNull)                 => i1
      case (JsNull, i2)                 => i2
      case (_, i2)                      => i2
    })

}
