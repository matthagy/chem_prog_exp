package chem_prog_exp

import chem_prog_exp.THREE.{LineBasicMaterial, Vector3}
import org.scalajs.dom
import org.scalajs.dom.html.Button

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation._


@JSExportTopLevel("Polymers")
object Polymers {

  private val boxSize: Double = 1.0
  private val halfBoxSize = boxSize / 2

  private var particleRadius: Double = 0

  private var bondFactor: Double = 0
  private var rBondZero: Double = 0

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
  private var visualizationRate: Int = 0

  private val boxColor = "green"
  private val particleColor = "orange"
  private val bondColor = "blue"


  private val zeroVector = Vector3(0)


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
    println("RUN: " + running)

    assert(running)
    println("HERE")
    copyParameters(parameters)

    val (scene: THREE.Scene, renderer: THREE.WebGLRenderer, camera: THREE.PerspectiveCamera) =
      createScence()

    val NPolymers = parameters.getOrElse("NPolymers", 50).asInstanceOf[Int]
    val polymerLength = parameters.getOrElse("polymerLength", 16).asInstanceOf[Int]
    val polymers = (1 to NPolymers).map(_ => createPolymer(polymerLength))
    polymers.foreach(_.addToScene(scene))

    val particles = polymers.flatMap(_.particles).toArray
    val bonds = polymers.flatMap(_.bonds).toArray

    var steps: Int = 0
    var neighbors = buildNeighbors(particles)

    // function to simulate the brownian propagation of the particle
    def advancePosition(): Unit = {
      if (steps % cycleDuration == 0)
        neighbors = buildNeighbors(particles)

      evaluateForces(particles, bonds, neighbors)
      particles.foreach(_.update())
      polymers.foreach(_.resolveSpherePositions())
      polymers.foreach(_.updateGeo())

      steps += 1
    }

    // animation loop function. called every time we update the visualization
    def animate(): Unit = {
      if (!running) return

      js.Dynamic.global.requestAnimationFrame(() => animate())
      renderer.render(scene, camera)

      for (_ <- 0 until visualizationRate)
        advancePosition()

      dom.document.getElementById("info").innerHTML =
        "Step: " + steps
    }

    animate() // start the animation loop
  }

  private def createScence() = {
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
    (scene, renderer, camera)
  }

  private def copyParameters(parameters: Dictionary[js.Object]): Unit = {
    particleRadius = parameters.getOrElse("particleRadius", 0.05).asInstanceOf[Double]

    bondFactor = parameters.getOrElse("bondFactor", 0.01).asInstanceOf[Double]
    rBondZero = parameters.getOrElse("rBondZero", 3 * particleRadius).asInstanceOf[Double]

    epsilon = parameters.getOrElse("epsilon", 0.00002).asInstanceOf[Double]
    sigma = parameters.getOrElse("sigma", 2 * particleRadius).asInstanceOf[Double]

    sigmaSqr = sigma * sigma
    LJFactor = -4 * 6 * epsilon

    rCutoff = 2.5 * sigma
    rCutoffSqr = rCutoff * rCutoff
    rNeighbor = rCutoff + 0.5

    randomScale = parameters.getOrElse("randomScale", 0.5 * particleRadius).asInstanceOf[Double]

    cycleDuration = parameters.getOrElse("cycleDuration", 5).asInstanceOf[Int]
    visualizationRate = parameters.getOrElse("visualizationRate", 5).asInstanceOf[Int]
  }

  private def createPolymer(polymerLength: Int): Polymer = {
    val startLocation = Vector3(randomCoordinate, -0.3, randomCoordinate)
    val color = particleColor //s"rgb($rndColorComponent, $rndColorComponent, $rndColorComponent)"
    val particles = (0 until polymerLength).map(i =>
      Particle(color, Vector3(startLocation.x, startLocation.y +  i * rBondZero, startLocation.z))
    ).toArray
    Polymer(color, particles)
  }

  private def rndColorComponent: Int =
    (256.0 * JsMath.uniformRandom).toInt

  case class Particle(color:String, position: Vector3) {
    val sphere : THREE.Mesh = createSphere(color, position)
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
    }
  }

  case class Polymer(color:String, particles: Array[Particle]) {
    private val lineGeo = createLineGeo(particles.length, color)
    updateGeo()

    def addToScene(scene: THREE.Scene): Unit = {
      for (particle <- particles) particle.addToScene(scene)
      scene.add(lineGeo)
    }

    def bonds: Array[(Particle, Particle)] = {
      (0 until particles.length - 1).map(i =>
        (particles(i), particles(i + 1))
      ).toArray
    }

    def resolveSpherePositions(): Unit = {
      particles(0).sphere.position.copy(particles(0).position)

      (1 until particles.length).foreach(index => {
        val lastPosition = particles(index - 1).sphere.position
        val particle = particles(index)
        val rij: Vector3 = evaluatePairVector(lastPosition, particle.position)
        particle.sphere.position.copy(lastPosition)
        particle.sphere.position.add(rij)
      })
    }

    def updateGeo(): Unit = {
      for (i <- particles.indices)
        lineGeo.geometry.vertices(i).copy(particles(i).sphere.position)
      lineGeo.geometry.verticesNeedUpdate = true
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
                     bonds: Array[(Particle, Particle)],
                     neighbors: Array[(Particle, Particle)]): Unit = {
    particles.foreach(_.zeroForce())

    for (t <- bonds) {
      val pI = t._1
      val pJ = t._2
      evaluateBondForces(pI.position, pJ.position) match {
        case (fi, fj) =>
          pI.addForce(fi)
          pJ.addForce(fj)
      }
    }

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

  def evaluateBondForces(va: Vector3, vb: Vector3): (Vector3, Vector3) = {
    val rij: Vector3 = evaluatePairVector(va, vb)

    // calculate the scalar force
    // scale it by 1/r such that it includes the normalization term to
    // make the separation vector a unit vector as needed to calculate
    // components of the force vector
    val r = JsMath.sqrt(rij.dot(rij))
    val f = bondFactor * (r - rBondZero)
    val scaledForce = f / r

    val forceA = Vector3.clone(rij)
    forceA.multiplyScalar(scaledForce)
    forceA.clampLength(0, maxForce)
    val forceB = Vector3.clone(rij)
    forceB.multiplyScalar(-scaledForce)
    forceB.clampLength(0, maxForce)
    (forceA, forceB)
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

  private def createLineGeo(vertices: Int, color: js.Any) = {
    val lineGeo = THREE.Geometry()
    (1 to vertices).foreach(_ => lineGeo.vertices.push(Vector3.clone(zeroVector)))
    THREE.Line(
      lineGeo,
      LineBasicMaterial(literal(color = color)),
    )
  }
}