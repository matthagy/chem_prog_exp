package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._


@JSExportTopLevel("RandomWalk")
object RandomWalk {

  private val boxSize = 1.0
  private val steps = 5000
  private val cycleDuration = 5

  private val halfBoxSize = boxSize / 2

  @JSExport
  def run(): Unit = {
    println("loaded")

    val scene = THREE.Scene()

    val camera = THREE.PerspectiveCamera(100, 1.2, 0.5, 10)
    camera.position.z = 1.5
    camera.position.x = 0.3
    camera.position.y = 0

    val renderer = THREE.WebGLRenderer()
    renderer.setSize(500, 500)
    dom.document.getElementById("render").appendChild(renderer.domElement)

    val cube = THREE.LineSegments(
      THREE.EdgesGeometry(THREE.BoxGeometry(boxSize, boxSize, boxSize)),
      THREE.LineBasicMaterial(js.Dynamic.literal(color = 0x00ff00))
    )
    cube.position.copy(THREE.Vector3(-halfBoxSize))
    scene.add(cube)

    def animate(): Unit = {
      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)
    }

    animate()
  }
}