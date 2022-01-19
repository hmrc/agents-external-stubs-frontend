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
      .map(path =>
        (
          path,
          path.foldLeft[JsLookupResult](JsDefined(json))((j, p) =>
            j match {
              case JsDefined(arr: JsArray) =>
                try JsDefined(arr(p.toInt))
                catch {
                  case NonFatal(_) => j
                }
              case JsDefined(_) => j \ p
              case _            => j
            }
          )
        )
      )
      .map {
        case (path, JsDefined(value)) =>
          path.init.foldRight[JsValue](
            if (isArrayIndex(path.last)) Json.arr(value)
            else Json.obj(path.last -> value)
          )((s, o) => if (isArrayIndex(s)) Json.arr(o) else Json.obj(s -> o))
        case _ => Json.obj()
      }
      .foldLeft[JsValue](Json.obj()) {
        case (o1: JsObject, o2: JsObject) => deepMerge(o1, o2)
        case (a1: JsArray, a2: JsArray)   => JsArray(a1.value ++ a2.value)
        case (x1, x2)                     => throw new UnsupportedOperationException(s"Could not merge $x1 and $x2")
      }

  private def isArrayIndex(s: String): Boolean = s == "*" || (try { s.toInt; true }
  catch { case NonFatal(_) => false })

  private def deepMerge(o1: JsObject, o2: JsObject): JsObject = {
    val result = o1.value ++ o2.value.map { case (otherKey, otherValue) =>
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

  sealed trait Tree extends Iterable[Tree] {
    def key: String
    def apply(prefix: String): Seq[Tree]
    def size: Int
  }
  case class Node(key: String, nodes: Seq[Tree]) extends Tree {
    override def apply(prefix: String): Seq[Tree] = nodes.filter(_.key.startsWith(key + prefix))
    override def iterator: Iterator[Tree] = nodes.iterator
    override def size: Int = nodes.size
    override def toString(): String = s"Node($key,${nodes.mkString("Seq(", ",", ")")})"
  }
  case class Leaf(key: String, value: String) extends Tree {
    override def apply(prefix: String): Seq[Tree] = Seq.empty
    override def iterator: Iterator[Tree] = Iterator.empty
    override def size: Int = 0
    override def toString(): String = s"Leaf($key,$value)"
  }

  def buildTree(key: String, data: Map[String, String]): Node =
    Node(
      key,
      data
        .groupBy { case (k, _) => k.takeWhile(_ != ']') }
        .toSeq
        .sortBy(_._1)
        .map { case (k, data1) =>
          val leafs = data1.collect {
            case (k1, v1) if k1.dropWhile(_ != ']').drop(1).isEmpty => Leaf(key + k1, v1)
          }.toSeq
          if (data1.size == 1 && leafs.size == 1) leafs.head
          else {
            val node = buildTree(
              key + k + ']',
              data1.collect {
                case (k1, v1) if k1.dropWhile(_ != ']').drop(1).nonEmpty => (k1.dropWhile(_ != ']').drop(1), v1)
              }
            )
            node.copy(nodes = leafs ++ node.nodes)
          }
        }
    )

}
