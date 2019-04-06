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

  private val boxSize: Double = 1.0

  private var particleRadius: Double = 0
  private var epsilon: Double = 0
  private var sigma: Double = 0

  private var rCutoff: Double = 0
  private var rCutoffSqr: Double = 0
  private var rNeighbor: Double = 0

  private var sigmaSqr: Double = 0
  private var LJFactor: Double = 0

  private var randomScale: Double = 0
  private val alpha = 1.0
  private val maxForce = 0.1 * 0.05

  private var cycleDuration: Int = 0
  private var analysisRate: Int = 0

  private val densityDistPrecision = 0.01
  private val densityDistMax = boxSize / 3
  private var pairDistanceCount: Array[Int] = null

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

  private var running: Boolean = false


  @JSExport
  def initialize(): Unit = {
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

        var editorValue = js.Dynamic.global.editor.getValue()
        js.Dynamic.global.eval(editorValue)
      }
    }
  }

  @JSExport
  def run(parameters: js.Dictionary[js.Object]): Unit = {
    assert(running)

    val nParticles = parameters.getOrElse("Nparticles", 1000).asInstanceOf[Int]
    particleRadius = parameters.getOrElse("particleRadius", 0.05).asInstanceOf[Double]
    epsilon = parameters.getOrElse("epsilon", 0.00002).asInstanceOf[Double]
    sigma = parameters.getOrElse("sigma", 2 * particleRadius).asInstanceOf[Double]
    randomScale = parameters.getOrElse("randomScale", 0.5 * particleRadius).asInstanceOf[Double]
    cycleDuration = parameters.getOrElse("cycleDuration", 5).asInstanceOf[Int]
    analysisRate = parameters.getOrElse("analysisRate", 5).asInstanceOf[Int]

    sigmaSqr = sigma * sigma
    LJFactor = -4 * 6 * epsilon

    rCutoff = 2.5 * sigma
    rCutoffSqr = rCutoff * rCutoff
    rNeighbor = rCutoff + 0.5

    pairDistanceCount =
      (0 to (densityDistMax / densityDistPrecision).toInt)
        .map(_ => 0)
        .toArray

    val scene = THREE.Scene()

    scene.add(THREE.AmbientLight(0x404040)) // soft white light

    val light = THREE.PointLight(0xff0000, 1, 0, 2)
    light.position.set(0, 0, 2)
    scene.add(light)

    val camera = THREE.PerspectiveCamera(70, 1, 0.8, 10)
    camera.position.copy(Vector3(0.1, 0, 1.5))

    val renderer = THREE.WebGLRenderer()
    renderer.setSize(500, 500)

    val renderDiv = dom.document.getElementById("render")
    while (renderDiv.firstChild != null)
      renderDiv.removeChild(renderDiv.firstChild)
    renderDiv.appendChild(renderer.domElement)

    scene.add(createSimulationBox)

    val particles = (1 to nParticles).map(_ => Particle(randomCoordinateVector)).toArray
    particles.foreach(_.addToScene(scene))

    var steps: Int = 0
    var neighbors = buildNeighbors(particles)

    // function to simulate the brownian propagation of the particle
    def advancePosition(): Unit = {
      if (steps % cycleDuration == 0)
        neighbors = buildNeighbors(particles)

      evaluateForces(particles, neighbors)
      particles.foreach(_.update())

      if (steps % analysisRate == 0)
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

    densityTraces = Nil

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


  }

  case class Particle(position: Vector3) {
    private val sphere = createSphere(particleColor, position)
    private val instantaneousForce = Vector3(0)

    def addToScene(scene: THREE.Scene): Unit = scene.add(sphere)

    def zeroForce(): Unit = instantaneousForce.copy(zeroVector)

    def addForce(force: Vector3): Unit = instantaneousForce.add(force)

    def update(): Unit = {
      val displacement = Vector3.clone(instantaneousForce)
      displacement.multiplyScalar(alpha)
      displacement.add(randomPerturbation)

      position.add(displacement)
      position.copy(periodize(position))

      sphere.position.copy(position)
    }
  }

  def buildNeighbors(particles: Array[Particle]): Array[(Particle, Particle)] = {
    var neighbors: mutable.ArrayBuilder[(Particle, Particle)] =
      new mutable.ArrayBuilder.ofRef[(Particle, Particle)]()

    val rNeighborSqr = rNeighbor * rNeighbor

    for (i <- particles.indices) {
      val particleI = particles(i)
      val positionI = particleI.position
      for (j <- i + 1 until particles.length) {
        val particleJ = particles(j)
        if (periodicDistanceSqr(positionI, particleJ.position) <=
          rNeighborSqr) {
          neighbors += ((particleI, particleJ))
        }
      }
    }
    neighbors.result()
  }


  def evaluateForces(particles: Array[Particle],
                     neighbors: Array[(Particle, Particle)]): Unit = {
    particles.foreach(_.zeroForce())

    for (t <- neighbors) {
      val pI = t._1
      val pJ = t._2
      evaluatePairForces(pI.position, pJ.position) match {
        case None =>
        case Some((fi, fj)) =>
          pI.addForce(fi)
          pJ.addForce(fj)
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

  def countPairDistances(particles: Array[Particle]): Unit = {
    for (i <- particles.indices) {
      val positionI = particles(i).position
      for (j <- i + 1 until particles.length) {
        val rij = evaluatePairVector(positionI, particles(j).position)
        val r = rij.length()
        val index = (r / densityDistPrecision).toInt
        if (index < pairDistanceCount.size) {
          pairDistanceCount(index) += 1
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

  @inline private final def periodizeOne(a: Double, b: Double): Double = {
    val l = b - a
    if (l > halfBoxSize) l - boxSize
    else if (l < -halfBoxSize) l + boxSize
    else l
  }

  @inline private final def evaluatePairVector(va: Vector3, vb: Vector3) = {
    Vector3(periodizeOne(va.x, vb.x), periodizeOne(va.y, vb.y), periodizeOne(va.z, vb.z))
  }

  @inline private final def periodicDistanceSqr(va: Vector3, vb: Vector3): Double = {
    @inline def sqr(x: Double) = x * x

    sqr(periodizeOne(va.x, vb.x)) +
      sqr(periodizeOne(va.y, vb.y)) +
      sqr(periodizeOne(va.z, vb.z))
  }


  private def randomCoordinate = boxSize * JsMath.uniformRandom

  private def randomCoordinateVector =
    Vector3(randomCoordinate, randomCoordinate, randomCoordinate)

  @inline private final def randomOffset: Double =
    randomScale * (JsMath.uniformRandom - 0.5)

  @inline private final def randomPerturbation: Vector3 =
    Vector3(randomOffset, randomOffset, randomOffset)

  /** Periodic box utilities
    */
  @inline private final def periodize(a: Double): Double = {
    if (a < -halfBoxSize) {
      periodize(a + boxSize)
    } else if (a > halfBoxSize) {
      periodize(a - boxSize)
    } else {
      a
    }
  }

  @inline private final def periodize(v: Vector3): Vector3 = {
    Vector3(periodize(v.x), periodize(v.y), periodize(v.z))
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