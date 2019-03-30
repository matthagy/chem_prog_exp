package chem_prog_exp

import scala.scalajs.js
import org.scalajs.dom.raw.Node

// Wrapper around the Javascript library THREE
// Inspired by https://github.com/totekp/threejs-scalajs-example/blob/master/src/main/scala/user/totekp/threejs/THREE.scala
object THREE {

  @js.native
  trait Vector3 extends js.Object {
    var x: js.JSNumberOps
    var y: js.JSNumberOps
    var z: js.JSNumberOps
    
    def copy(v: Vector3): Unit

    def add(v: Vector3): Unit
  }

  object Vector3 {
    def apply(x: js.JSNumberOps, y: js.JSNumberOps, z: js.JSNumberOps): Vector3 =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Vector3)(x, y, z).asInstanceOf[Vector3]

    def unapply(v: Vector3): Option[(js.JSNumberOps, js.JSNumberOps, js.JSNumberOps)] =
      Some((v.x, v.y, v.z))

    def clone(v: Vector3) = Vector3(v.x, v.y, v.z)
  }

  @js.native
  trait SceneObject extends js.Object

  @js.native
  trait Scene extends js.Object {
    def add(so: SceneObject): Unit
  }

  object Scene {
    def apply(): Scene =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Scene)().asInstanceOf[Scene]
  }

  @js.native
  trait Camera extends js.Object

  @js.native
  trait PerspectiveCamera extends Camera {
    val position: Vector3
  }

  object PerspectiveCamera {
    def apply(fov: js.JSNumberOps,
              aspect: js.JSNumberOps,
              near: js.JSNumberOps,
              far: js.JSNumberOps): PerspectiveCamera =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.PerspectiveCamera)(
        fov, aspect, near, far
      ).asInstanceOf[PerspectiveCamera]
  }


  @js.native
  trait WebGLRenderer extends js.Object {
    def setSize(width: js.JSNumberOps, height: js.JSNumberOps)

    def render(scene: Scene, camera: Camera)

    val domElement: Node
  }

  object WebGLRenderer {
    def apply(): WebGLRenderer =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.WebGLRenderer)().asInstanceOf[WebGLRenderer]
  }

  @js.native
  trait Geometry extends js.Object

  @js.native
  trait BoxGeometry extends Geometry

  object BoxGeometry {
    def apply(x:js.JSNumberOps, y:js.JSNumberOps, z:js.JSNumberOps): BoxGeometry =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.BoxGeometry)(
        x, y, z
      ).asInstanceOf[BoxGeometry]
  }

  @js.native
  trait EdgesGeometry extends Geometry

  object EdgesGeometry {
    def apply(geometry: Geometry): EdgesGeometry =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.EdgesGeometry)(
        geometry
      ).asInstanceOf[EdgesGeometry]
  }


  @js.native
  trait Material extends js.Object

  @js.native
  trait LineBasicMaterial extends Material

  object LineBasicMaterial {
    def apply(attrs: js.Object): LineBasicMaterial =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.LineBasicMaterial)(
        attrs
      ).asInstanceOf[LineBasicMaterial]
  }

  @js.native
  trait LineSegments extends SceneObject

  object LineSegments {
    def apply(geometry: Geometry, material: Material): LineSegments =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.LineSegments)(
        geometry, material
      ).asInstanceOf[LineSegments]
  }
}
