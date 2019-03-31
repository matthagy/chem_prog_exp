package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._
import js.Dynamic.literal

import chem_prog_exp.THREE.Vector3


@JSExportTopLevel("RandomWalk")
object RandomWalk {

  private val boxSize = 1.0
  private val steps = 5000
  private val cycleDuration = 5

  private val halfBoxSize = boxSize / 2

  private val zeroVector = Vector3(0)
  private val truePositions = Vector3.clone(zeroVector)
  private val periodicPosition = Vector3.clone(zeroVector)

  @JSExport
  def run(): Unit = {
    println("loaded")

    val scene = THREE.Scene()

    val camera = THREE.PerspectiveCamera(70, 1, 0.1, 10)
    camera.position.copy(Vector3(0.1, 0, 1.5))

    val renderer = THREE.WebGLRenderer()
    renderer.setSize(500, 500)
    dom.document.getElementById("render").appendChild(renderer.domElement)

    scene.add(createSimulationCube)
    scene.add(createTracerSphere("red", Vector3(0, 0, 0)))
    scene.add(createTracerSphere("blue", Vector3(1, 1, 1)))
    scene.add(createTracerSphere("yellow", Vector3(-1, -1, -1)))
    scene.add(createTracerSphere("teal", Vector3(-halfBoxSize)))

    def animate(): Unit = {
      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)
    }

    animate()
  }

  private def createSimulationCube = {
    val cube = THREE.LineSegments(
      THREE.EdgesGeometry(THREE.BoxGeometry(boxSize, boxSize, boxSize)),
      THREE.LineBasicMaterial(literal(color = 0x00ff00)))
    cube.position.copy(zeroVector)
    cube
  }

  private def createTracerSphere(color: js.Any, position: Vector3 = zeroVector) = {
    val sphere = THREE.Mesh(
      THREE.SphereGeometry(0.025 * boxSize, 32, 32),
      THREE.MeshBasicMaterial(literal(color = color)))
    sphere.position.copy(position)
    sphere
  }
}