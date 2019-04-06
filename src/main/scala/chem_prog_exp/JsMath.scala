package chem_prog_exp

import scala.scalajs.js

object JsMath {
   @inline final def sqrt(x: Double) : Double =
    js.Dynamic.global.Math.sqrt(x).asInstanceOf[Double]

   @inline final def log(x: Double) : Double =
    js.Dynamic.global.Math.log(x).asInstanceOf[Double]

   @inline final def floor(x: Double) : Double =
    js.Dynamic.global.Math.floor(x).asInstanceOf[Double]

   @inline final def uniformRandom : Double =
    js.Dynamic.global.Math.random().asInstanceOf[Double]
}
