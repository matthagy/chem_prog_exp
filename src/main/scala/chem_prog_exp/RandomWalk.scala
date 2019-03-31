package chem_prog_exp

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation._
import js.Dynamic.literal
import chem_prog_exp.THREE.{LineBasicMaterial, Vector3}


@JSExportTopLevel("RandomWalk")
object RandomWalk {

  private val boxSize = 1.0
  private val cycleDuration = 5

  private val halfBoxSize = boxSize / 2

  private val zeroVector = Vector3(0)

  @JSExport
  def run(): Unit = {
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

    val deltaLine = createLineGeo(2, "red")
    scene.add(deltaLine)

    // function to simulate the brownian propagation of the particle
    def advancePosition(): Unit = {
      (1 to cycleDuration).foreach(_ => {
        val delta = randomVector

        trueTrajectory.updatePosition(delta)

        periodicTrajectory.updatePosition(delta)
        perodizeVector(periodicTrajectory.position)

        updateLineGeo(deltaLine, 1, trueTrajectory.position)
      })
    }

    // re-draw the plot
    // note we should be using the method that only updates the plot, but I
    // can't seem to get that to work and see tickets about the issue
    // instead, I'm stupidly re-drawing the full plot each time, which is expensive
    def plot(): Unit = {
      js.Dynamic.global.Plotly.newPlot(
        "plot",
        js.Array(tracePoints, traceLine),
        plotLayout,
        literal(showSendToCloud = false)
      )
    }

    plot()

    var index = 0

    // animation loop function. called every time we update the visualization
    def animate(): Unit = {
      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)

      advancePosition()

      val precision = 0.001
      val displacement = js.Dynamic.global.Math.floor(
        trueTrajectory.position.length() / precision)
      dom.document.getElementById("info").innerHTML =
        "Step: " + index + " Displacement: " + displacement

      index += 1

      tracePoints.x.push(index)
      tracePoints.y.push(displacement)
      if (index % 100 == 0) {
        traceLine.x.push(index)
        traceLine.y.push(displacement)
      }
      if (index % 25 == 0) {
        plot()
      }
    }

    animate() // start the animation loop
  }

  /** Visualization utilities
    */

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

  private def updateLineGeo(lineGeo: THREE.Line, index: Int, vector3: Vector3): Unit = {
    lineGeo.geometry.vertices(index).copy(vector3)
    lineGeo.geometry.verticesNeedUpdate = true
  }

  /** Represents the path taken by a particle, including it's history,
    * for visualization
    */

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

    def updatePosition(delta: Vector3): Unit = {
      position.add(delta)
      tracer.position.copy(position)
      currentSegmentIndex += 1
      if (currentSegmentIndex < maxSegments) {
        updateLineGeo(segment, currentSegmentIndex, position)
      }
    }
  }

  /** Random number utilities
    */

  def randomOffset: Double = 0.05 * boxSize *
    (js.Dynamic.global.Math.random().asInstanceOf[Double] - 0.5)

  def randomVector: Vector3 = Vector3(randomOffset, randomOffset, randomOffset)

  /** Periodic box utilities
    */

  def periodize(a: Double): Double = {
    if (a < -halfBoxSize) {
      a + boxSize
    } else if (a > halfBoxSize) {
      a - boxSize
    } else {
      a
    }
  }

  def perodizeVector(v: Vector3): Unit = {
    v.x = periodize(v.x)
    v.y = periodize(v.y)
    v.z = periodize(v.z)
  }

  /** Definitions of the plot
    * mainly just a lot of JavaScript literals for interpretation by plotly
    */

  private val tracePoints = literal(
    x = js.Array(0),
    y = js.Array(0),
    name = "Points",
    `type` = "Scatter",
    mode = "markers",
    marker = literal(
      color = "rgb(164, 194, 244)",
      size = 5
    )
  )

  private val traceLine = literal(
    x = js.Array(0),
    y = js.Array(0),
    name = "Smoothed",
    `type` = "Scatter",
    mode = "line",
    line = literal(
      shape = "spline",
      color = "rgb(255,127,80)",
      width = 3
    )
  )

  private val fontFamily = "Courier New, monospace"

  private val plotLayout = literal(
    height = 300,
    title = literal(
      text = "Root Mean Square Displacement of Tracer",
      font = literal(
        family = fontFamily,
        size = 18
      ),
      xref = "paper",
      x = 0.05
    ),
    xaxis = axis("Time, t (steps)"),
    yaxis = axis("RMS Displacement")
  )

  private def axis(text: String) = {
    literal(
      title = literal(
        text = text,
        font = literal(
          family = fontFamily,
          size = 14,
          color = "#7f7f7f"
        )
      )
    )
  }
}