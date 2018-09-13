package uk.gov.hmrc.agentsexternalstubsfrontend.views

import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.agentsexternalstubsfrontend.ViewHelper
import uk.gov.hmrc.play.test.UnitSpec

class ViewHelperSpec extends UnitSpec {

  "ViewHelper" should {
    "prepare an excerpt of JsObject" in {
      ViewHelper.excerpt(Json.obj()) shouldBe Json.obj()
      ViewHelper.excerpt(Json.obj("a" -> "b")) shouldBe Json.obj()
      ViewHelper.excerpt(Json.obj("a" -> "b"), "a") shouldBe Json.obj("a" -> "b")
      ViewHelper.excerpt(Json.obj("a" -> "b"), "b") shouldBe Json.obj()
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c")), "a") shouldBe Json.obj("a" -> Json.obj("b" -> "c"))
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c")), "a.b") shouldBe Json.obj("a" -> Json.obj("b" -> "c"))
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c", "d" -> "e")), "a.b") shouldBe Json.obj(
        "a" -> Json.obj("b" -> "c"))
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c", "d" -> "e"), "f" -> "g"), "a.b") shouldBe Json.obj(
        "a" -> Json.obj("b" -> "c"))
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c", "d" -> "e"), "f" -> "g"), "a.b", "f") shouldBe Json
        .obj("a" -> Json.obj("b" -> "c"), "f" -> "g")
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c", "d" -> "e"), "f" -> "g"), "a.b", "f.g") shouldBe Json
        .obj("a" -> Json.obj("b" -> "c"))
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c", "d" -> "e"), "f" -> "g"), "f.g") shouldBe Json
        .obj()
      ViewHelper.excerpt(Json.obj("a" -> Json.obj("b" -> "c")), "a.c") shouldBe Json.obj()
      ViewHelper.excerpt(Json.obj("a" -> JsArray(Seq(Json.obj("b" -> "c")))), "a.0") shouldBe Json
        .obj("a" -> Json.arr(Json.obj("b" -> "c")))
      ViewHelper.excerpt(Json.obj("a" -> JsArray(Seq(Json.obj("b" -> "c")))), "a.0.b") shouldBe Json
        .obj("a" -> Json.arr(Json.obj("b" -> "c")))
      ViewHelper.excerpt(Json.obj("a" -> JsArray(Seq(Json.obj("b" -> "c", "d" -> "e")))), "a.0.b") shouldBe Json
        .obj("a" -> Json.arr(Json.obj("b" -> "c")))
      ViewHelper
        .excerpt(Json.obj("a" -> JsArray(Seq(Json.obj("b" -> "c", "d" -> "e")))), "a.0.b", "a.0.d") shouldBe Json
        .obj("a" -> Json.arr(Json.obj("b" -> "c", "d" -> "e")))
      ViewHelper
        .excerpt(Json.obj("a" -> JsArray(Seq(Json.obj("b" -> "c", "d" -> "e", "f" -> "g")))), "a.0.b", "a.0.f") shouldBe Json
        .obj("a" -> Json.arr(Json.obj("b" -> "c", "f" -> "g")))
    }
  }

}
