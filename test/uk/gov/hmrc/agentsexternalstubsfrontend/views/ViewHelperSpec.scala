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

    "build a tree out of map" in {
      import ViewHelper.{Leaf, Node}
      ViewHelper.buildTree("", Map()) shouldBe Node("", Seq.empty)
      ViewHelper.buildTree("", Map("foo"  -> "bar")) shouldBe Node("", Seq(Leaf("foo", "bar")))
      ViewHelper.buildTree("", Map("foo1" -> "bar1", "foo2" -> "bar2")) shouldBe Node(
        "",
        Seq(Leaf("foo1", "bar1"), Leaf("foo2", "bar2")))
      ViewHelper
        .buildTree("", Map("foo[0]" -> "bar")) shouldBe Node("", Seq(Leaf("foo[0]", "bar")))
      ViewHelper
        .buildTree("", Map("foo[0].zoo" -> "bar")) shouldBe Node(
        "",
        Seq(Node("foo[0]", Seq(Leaf("foo[0].zoo", "bar")))))
      ViewHelper.buildTree("", Map("foo[0]" -> "bar0", "foo[1]" -> "bar1")) shouldBe Node(
        "",
        Seq(Leaf("foo[0]", "bar0"), Leaf("foo[1]", "bar1")))
      ViewHelper
        .buildTree("", Map("foo[0].zoo[0]" -> "bar0zoo0", "foo[0].zoo[1]" -> "bar0zoo1", "foo[1]" -> "bar1")) shouldBe Node(
        "",
        Seq(
          Node("foo[0]", Seq(Leaf("foo[0].zoo[0]", "bar0zoo0"), Leaf("foo[0].zoo[1]", "bar0zoo1"))),
          Leaf("foo[1]", "bar1")
        )
      )
      ViewHelper
        .buildTree(
          "",
          Map(
            "foo[0].zoo[0].key"   -> "bar0zoo0key",
            "foo[0].zoo[0].value" -> "bar0zoo0value",
            "foo[0].zoo[1].key"   -> "bar0zoo1key",
            "foo[0].zoo[1].value" -> "bar0zoo1value",
            "foo[1]"              -> "bar1"
          )
        ) shouldBe Node(
        "",
        Seq(
          Node(
            "foo[0]",
            Seq(
              Node(
                "foo[0].zoo[0]",
                Seq(Leaf("foo[0].zoo[0].key", "bar0zoo0key"), Leaf("foo[0].zoo[0].value", "bar0zoo0value"))),
              Node(
                "foo[0].zoo[1]",
                Seq(Leaf("foo[0].zoo[1].key", "bar0zoo1key"), Leaf("foo[0].zoo[1].value", "bar0zoo1value")))
            )
          ),
          Leaf("foo[1]", "bar1")
        )
      )
    }
    "iterate over the tree" in {
      val tree = ViewHelper
        .buildTree("", Map("foo[0].zoo[0]" -> "bar0zoo0", "foo[0].zoo[1]" -> "bar0zoo1", "foo[1]" -> "bar1"))
      tree.iterator.size shouldBe 2
      tree("foo").iterator.size shouldBe 2
      tree("foo").flatMap(_.apply(".zoo")).iterator.size shouldBe 2
    }
  }

}
