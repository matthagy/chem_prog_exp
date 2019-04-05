package chem_prog_exp

import chem_prog_exp.THREE.Vector3
import org.scalajs.dom
import org.scalajs.dom.html.Button

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation._


@JSExportTopLevel("SoftSpheres")
object SoftSpheres {

  private val boxSize = 1.0
  private val particleRadius = 0.05 * boxSize
  private val nParticles = 1000

  private val cycleDuration = 1

  private val sigma = 2 * particleRadius
  private val epsilon = 0.00002
  private val sigmaSqr = sigma * sigma
  private val LJFactor = -4 * 6 * epsilon
  private val rCutoff = 2.5 * sigma
  private val rCutoffSqr = rCutoff * rCutoff

  private val randomScale = 0.5 * particleRadius
  private val alpha = 1.0
  private val maxForce = 0.1 * particleRadius

  private val densityDistPrecision = 0.1 * particleRadius
  private val densityDistMax = boxSize / 2
  private val pairDistanceCount =
    (0 to (densityDistMax / densityDistPrecision).toInt)
      .map(_ => 0)
      .toArray
  private val plotDistances: Set[Int] = Set(
    1, 10, 50, 100, 250, 500, 1000, 1500, 2000)

  private val boxColor = "green"
  private val particleColor = "orange"

  private val halfBoxSize = boxSize / 2

  private val zeroVector = Vector3(0)

  private var densityTraces: List[js.Object] = Nil

  /** Definitions of the plot
    * mainly just a lot of JavaScript literals for interpretation by plotly
    */

  private val fontFamily = "Courier New, monospace"

  private val plotLayout = literal(
    height = 300,
    title = literal(
      text = "Reduced Pair Density Distribution, g(r)",
      font = literal(
        family = fontFamily,
        size = 18
      ),
      xref = "paper",
      x = 0.05
    ),
    xaxis = axis("Pair Distance, r, (Particle Radius)"),
    yaxis = axis("g(r)")
  )

  @JSExport
  def run(): Unit = {
    val scene = THREE.Scene()

    scene.add(THREE.AmbientLight(0x404040)) // soft white light

    val light = THREE.PointLight(0xff0000, 1, 0, 2)
    light.position.set(0, 0, 2)
    scene.add(light)

    val camera = THREE.PerspectiveCamera(70, 1, 0.8, 10)
    camera.position.copy(Vector3(0.1, 0, 1.5))

    val renderer = THREE.WebGLRenderer()
    renderer.setSize(500, 500)
    dom.document.getElementById("render").appendChild(renderer.domElement)

    scene.add(createSimulationBox)

    val particles = (1 to nParticles).map(_ => Particle(randomCoordinateVector)).toArray
    particles.foreach(_.addToScene(scene))

    // function to simulate the brownian propagation of the particle
    def advancePosition(): Unit = {
      println("update")

      (1 to cycleDuration).foreach(_ => {
        evaluateForce(particles)
        particles.foreach(_.update())
      })
      countPairDistances(particles)
    }

    // re-draw the plot
    // note we should be using the method that only updates the plot, but I
    // can't seem to get that to work and see tickets about the issue
    // instead, I'm stupidly re-drawing the full plot each time, which is expensive
    def plot(): Unit = {
      js.Dynamic.global.Plotly.newPlot(
        "plot",
        js.Array(densityTraces: _*),
        plotLayout,
        literal(showSendToCloud = false)
      )
    }

    var steps: Int = 0
    var running = true

    // animation loop function. called every time we update the visualization
    def animate(): Unit = {
      if (!running) return

      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)

      advancePosition()

      steps += 1

      dom.document.getElementById("info").innerHTML =
        "Step: " + steps

      //val stepsBase5 = log(steps) / log(5)
      // if (stepsBase5 - floor(stepsBase5) < 1e-6) {
      if (plotDistances.contains(steps)) {
        densityTraces = createPairDensityTrace(steps, "Steps: " + steps) :: densityTraces
        plot()
      }
    }

    animate() // start the animation loop


    val startStop = dom.document.getElementById("start_stop")
    startStop
      .asInstanceOf[Button]
      .onclick = event => {
      if (running) {
        startStop.innerHTML = "Start"
        running = false
      } else {
        startStop.innerHTML = "Stop"
        running = true
        animate()
      }
    }
  }

  case class Particle(position: Vector3) {
    private val sphere = createSphere(particleColor, position)
    private val neighbors: List[Particle] = Nil
    private val instantaneousForce = Vector3(0)

    def addToScene(scene: THREE.Scene): Unit = scene.add(sphere)

    def zeroForce(): Unit = instantaneousForce.copy(zeroVector)

    def addForce(force: Vector3): Unit = instantaneousForce.add(force)

    def update(): Unit = {
      //println("instance force", instantaneousForce.length())
      val displacement = Vector3.clone(instantaneousForce)
      displacement.multiplyScalar(alpha)
      displacement.add(randomPerturbation)
      //println("displacement", displacement.length())

      position.add(displacement)
      position.copy(periodize(position))
      //println("position " + position.x + "," + position.y + "," + position.z)

      sphere.position.copy(position)
    }
  }

  def evaluateForce(particles: Array[Particle]): Unit = {
    particles.foreach(_.zeroForce())

    for (i <- particles.indices) {
      val positionI = particles(i).position
      for (j <- i + 1 until particles.length) {
        evaluatePairForces(positionI, particles(j).position) match {
          case None =>
          case Some((fi, fj)) =>
            particles(i).addForce(fi)
            particles(j).addForce(fj)
        }
      }
    }
  }

  def evaluatePairForces(va: Vector3, vb: Vector3): Option[(Vector3, Vector3)] = {
    val rij: Vector3 = evaluatePairVector(va, vb)

    val rsqr = rij.dot(rij)
    if (rsqr > rCutoffSqr) None
    else {
      // calculate the scalar force
      // scale it by 1/r such that it includes the normalization term to
      // make the separation vector a unit vector as needed to calculate
      // components of the force vector
      val invRSqr = 1.0 / rsqr
      val x2 = sigmaSqr * invRSqr
      val x6 = x2 * x2 * x2
      val scaledForce = LJFactor * invRSqr * (2 * x6 * x6 - x6)
      //println("rsqr " + sqrt(rsqr) / particleRadius + " force " + scaledForce)

      val forceA = Vector3.clone(rij)
      forceA.multiplyScalar(scaledForce)
      forceA.clampLength(0, maxForce)
      val forceB = Vector3.clone(rij)
      forceB.multiplyScalar(-scaledForce)
      forceB.clampLength(0, maxForce)
      Some((forceA, forceB))
    }
  }

  private def evaluatePairVector(va: Vector3, vb: Vector3) = {
    def periodizeOne(a: Double, b: Double): Double = {
      val l = b - a
      if (l > halfBoxSize) l - boxSize
      else if (l < -halfBoxSize) l + boxSize
      else l
    }

    Vector3(periodizeOne(va.x, vb.x), periodizeOne(va.y, vb.y), periodizeOne(va.z, vb.z))
  }

  def countPairDistances(particles: Array[Particle]): Unit = {
    for (i <- particles.indices) {
      val positionI = particles(i).position
      for (j <- i + 1 until particles.length) {
        val rij = evaluatePairVector(positionI, particles(j).position)
        val r = rij.length()
        if (r < densityDistMax) {
          pairDistanceCount((r / densityDistPrecision).toInt) += 1
        }
      }
    }
  }

  def createPairDensityTrace(steps: Int, title: String): js.Object = {
    val volumeFactor = 4.0 / 3.0 * 3.1415

    def shellDensity(index: Int): Double = {
      val r = index * densityDistPrecision
      volumeFactor * r * r * r
    }

    val N = pairDistanceCount.sum
    val V = volumeFactor * densityDistMax * densityDistMax * densityDistMax
    val bulkDensity = N / V

    val reducedDensity = pairDistanceCount.indices.map(i => {
      val count = pairDistanceCount(i)
      val volume = shellDensity(i + 1) - shellDensity(i)
      count / volume / bulkDensity
    })

    val r = reducedDensity.indices.map(_ * densityDistPrecision / particleRadius)
    literal(
      x = js.Array(r: _*),
      y = js.Array(reducedDensity: _*),
      name = title,
      `type` = "Scatter",
      mode = "line",
      line = literal(
        shape = "spline",
        //color = "rgb(255,127,80)",
        width = 2
      )
    )
  }


  case class NeighborListBuilder() {
    private val grid: mutable.HashMap[(Int, Int), Particle] = mutable.HashMap.empty
  }

  /** Visualization utilities
    */
  private def createSimulationBox = {
    val cube = THREE.LineSegments(
      THREE.EdgesGeometry(THREE.BoxGeometry(boxSize)),
      THREE.LineBasicMaterial(literal(color = boxColor)))
    cube.position.copy(zeroVector)
    cube
  }

  private def createSphere(color: js.Any, position: Vector3 = zeroVector) = {
    val sphere = THREE.Mesh(
      THREE.SphereGeometry(particleRadius, 8, 8),
      THREE.MeshLambertMaterial(literal(color = color)))
    sphere.position.copy(position)
    sphere
  }

  private def sqrt(x: Double) =
    js.Dynamic.global.Math.sqrt(x).asInstanceOf[Double]

  private def log(x: Double) =
    js.Dynamic.global.Math.log(x).asInstanceOf[Double]

  private def floor(x: Double) =
    js.Dynamic.global.Math.floor(x).asInstanceOf[Double]

  /** Random number utilities
    */
  private def uniformRandom =
    js.Dynamic.global.Math.random().asInstanceOf[Double]

  private def randomCoordinate = boxSize * uniformRandom

  private def randomCoordinateVector =
    Vector3(randomCoordinate, randomCoordinate, randomCoordinate)

  private def randomOffset: Double = randomScale * (uniformRandom - 0.5)

  private def randomPerturbation: Vector3 = Vector3(randomOffset, randomOffset, randomOffset)

  /** Periodic box utilities
    */
  def periodize(a: Double): Double = {
    if (a < -halfBoxSize) {
      periodize(a + boxSize)
    } else if (a > halfBoxSize) {
      periodize(a - boxSize)
    } else {
      a
    }
  }

  def periodize(v: Vector3): Vector3 = {
    Vector3(periodize(v.x), periodize(v.y), periodize(v.z))
  }

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