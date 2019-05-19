package chem_prog_exp

import scala.scalajs.js

object JsMath {
   @inline final def sqrt(x: Double) : Double =
    js.Dynamic.global.Math.sqrt(x).asInstanceOf[Double]

   @inline final def log(x: Double) : Double =
    js.Dynamic.global.Math.log(x).asInstanceOf[Double]

   @inline final def floor(x: Double) : Double =
    js.Dynamic.global.Math.floor(x).asInstanceOf[Double]

  @inline final def exp(x: Double) : Double =
    js.Dynamic.global.Math.exp(x).asInstanceOf[Double]

   @inline final def uniformRandom : Double =
    js.Dynamic.global.Math.random().asInstanceOf[Double]

  @inline final def randomRange(inclusiveStart:Int, exclusiveEnd:Int) : Int =
    floor(uniformRandom * (exclusiveEnd-inclusiveStart) + inclusiveStart).toInt

  @inline final def randomRange(exclusiveEnd:Int) : Int =
    floor(uniformRandom * exclusiveEnd).toInt

  @inline final def randomBoolean : Boolean =
    uniformRandom > 0.5
}
