package chem_prog_exp

import org.scalajs.dom
import org.scalajs.dom.html.{Button, Canvas}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation._


@JSExportTopLevel("Ising")
object Ising {

  private val canvasSize: Int = 1000

  private var lateralSize: Int = 0
  private var pairInteraction: Double = 0
  private var externalField: Double = 0

  private var visualizationRate: Int = 0

  private var spins: Array[Int] = null

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

    lateralSize = parameters.getOrElse("lateralSize", 10).asInstanceOf[Int]
    pairInteraction = parameters.getOrElse("pairInteraction", 1.0).asInstanceOf[Double]
    externalField = parameters.getOrElse("externalField", 1.0).asInstanceOf[Double]
    println("pairInteraction: " + pairInteraction)
    println("externalField: " + externalField)

    visualizationRate = parameters.getOrElse("visualizationRate", 5).asInstanceOf[Int]

    spins = new Array[Int](lateralSize * lateralSize)
    for (i <- 0 until lateralSize * lateralSize)
      spins(i) = if (JsMath.randomBoolean) 1 else -1

    draw()

    var steps: Int = 0
    var accepts: Int = 0

    // animation loop function. called every time we update the visualization
    def animate(): Unit = {
      if (!running) return

      js.Dynamic.global.requestAnimationFrame(() => animate())

      for (i <- 0 until visualizationRate) {
        if (advanceSimulation()) accepts += 1
        steps += 1
      }
      draw()

      dom.document.getElementById("info").innerHTML =
        "Step: " + steps +
          " Net spin: " + spins.sum +
          " Acc Rate: " + (JsMath.floor(100 * accepts / steps) / 100)
    }

    animate() // start the animation loop
  }

  def draw(): Unit = {
    val canvas = dom.document.getElementById("canvas").asInstanceOf[Canvas]
    val ctx = canvas.getContext("2d")
    val cellSize = canvasSize.toDouble / lateralSize.toDouble

    for (i <- 0 until lateralSize) {
      for (j <- 0 until lateralSize) {
        val spin = spins(i * lateralSize + j)
        ctx.fillStyle = spin match {
          case 1 => "blue"
          case -1 => "red"
        }
        ctx.fillRect(i * cellSize, j * cellSize, cellSize, cellSize)
      }
    }
  }

  def advanceSimulation(): Boolean = {
    val i = JsMath.randomRange(lateralSize)
    val j = JsMath.randomRange(lateralSize)
    val spin = spins(i * lateralSize + j)
    val newSpin = spin match {
      case 1 => -1
      case -1 => 1
    }

    val dSpin = (newSpin - spin).toDouble
    var dU = -externalField * dSpin

    def pairEnergy(di: Int, dj: Int): Unit = {
      val ni = if (di == 0) i else periodize(i + di)
      val nj = if (dj == 0) j else periodize(j + dj)
      dU += -pairInteraction * spins(ni * lateralSize + nj).toDouble * dSpin
    }

    pairEnergy(0, 1)
    pairEnergy(1, 0)
    pairEnergy(0, -1)
    pairEnergy(-1, 0)

    if (js.Dynamic.global.Number.isNaN(dU).asInstanceOf[Boolean]) {
      println("NAN DU")
      println("i,j=" + i + "," + j)
      println("spin=" + spin)
      println("newSpin=" + newSpin)
      throw new RuntimeException("NAN DU")
    }

    //println("dU: " + dU)

    if (dU <= 0 || JsMath.exp(-dU) > JsMath.uniformRandom) {
      spins(i * lateralSize + j) = newSpin
      //println("accept")
      true
    } else {
      //println("reject")
      false
    }
  }

  def periodize(x: Int): Int = {
    if (x >= lateralSize) {
      x - lateralSize
    } else if (x < 0) {
      x + lateralSize
    } else {
      x
    }
  }
}