package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._
import js.Dynamic.literal
import chem_prog_exp.THREE.{Geometry, LineBasicMaterial, Vector3}


@JSExportTopLevel("RandomWalk")
object RandomWalk {

  private val boxSize = 1.0
  private val steps = 5000
  private val cycleDuration = 5

  private val halfBoxSize = boxSize / 2

  private val zeroVector = Vector3(0)
  private val truePosition = Vector3.clone(zeroVector)
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

    val trueTrajectory = Trajectory("red", "orange")
    val periodicTrajectory = Trajectory("teal", "blue")
    // Render true on top of periodic
    periodicTrajectory.addToScene(scene)
    trueTrajectory.addToScene(scene)

    val testPosition = Vector3.clone(zeroVector)
    def animate(): Unit = {
      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)
      testPosition.x = testPosition.x + 0.01
      trueTrajectory.updatePosition(testPosition)
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

  private def createLineGeo(segments: Int, color: js.Any) = {
    val lineGeo = THREE.Geometry()
    (1 to segments).foreach(_ => lineGeo.vertices.push(Vector3.clone(zeroVector)))
    THREE.Line(
      lineGeo,
      LineBasicMaterial(literal(color = color)),
    )
  }

  private def updateLineGeo(lineGeo: THREE.Line, index:Int, vector3: Vector3): Unit = {
    lineGeo.geometry.vertices(index).copy(vector3)
    lineGeo.geometry.verticesNeedUpdate = true
  }

  // Represents the path taken by a particle, including it's history
  case class Trajectory(tracerColor: js.Any,
                        segmentColor: js.Any,
                        initialPosition: Vector3 = zeroVector,
                        maxSegments: Int = 10 * 1000) {

    val position: Vector3 = Vector3.clone(initialPosition)
    private val tracer = createTracerSphere(tracerColor, position)
    private val segment = createLineGeo(maxSegments, segmentColor)
    private var currentSegmentIndex = 0

    def addToScene(scene: THREE.Scene): Unit = {
      scene.add(tracer)
      scene.add(segment)
    }

    def updatePosition(newPosition: Vector3): Unit = {
      position.copy(newPosition)
      currentSegmentIndex += 1
      if (currentSegmentIndex < maxSegments) {
        updateLineGeo(segment, currentSegmentIndex, newPosition)
      }
    }
  }
}