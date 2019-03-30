package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._


@JSExportTopLevel("RandomWalk")
object RandomWalk {
  @JSExport
  def run(): Unit = {
    println("loaded")

    val v = THREE.Vector3(0, 1, 2)
    println(v.x + "," + v.y + "," + v.z)
    v.add(THREE.Vector3(1, 2, 3))
    println(v.x + "," + v.y + "," + v.z)

    val scene = THREE.Scene()

    val camera = THREE.PerspectiveCamera(100, 1.2, 0.5, 10)
    camera.position.z = 1.5
    camera.position.x = 0.3
    camera.position.y = 0

    val renderer = THREE.WebGLRenderer()
    renderer.setSize(500, 500)

    dom.document.getElementById("render").appendChild(renderer.domElement)

  }
}