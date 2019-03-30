package chem_prog_exp

import org.scalajs.dom.Node

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSName, ScalaJSDefined}

// Wrapper around the Javascript library THREE
// Inspired by https://github.com/totekp/threejs-scalajs-example/blob/master/src/main/scala/user/totekp/threejs/THREE.scala
object THREE {

  @js.native
  trait Vector3 extends js.Object {
    var x: js.JSNumberOps
    var y: js.JSNumberOps
    var z: js.JSNumberOps

    def add(v: Vector3): Unit
  }

  object Vector3 {
    def apply(x: js.JSNumberOps, y: js.JSNumberOps, z: js.JSNumberOps): Vector3 =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Vector3)(x, y, z).asInstanceOf[Vector3]
  }
}
