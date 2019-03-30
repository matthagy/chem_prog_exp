package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

object Main {
  @JSExport
  def main(args: Array[String]): Unit = {
    dom.window.onload = e => {
      println("loaded")
      println(js.Dynamic.global.THREE.Vector3)
      val v = THREE.Vector3(0, 1, 2)
      println(v.x + "," + v.y + "," + v.z)
      v.add(THREE.Vector3(1, 2, 3))
      println(v.x + "," + v.y + "," + v.z)
    }
  }
}