package chem_prog_exp

import scala.scalajs.js
import org.scalajs.dom.raw.Node


/** Partial wrapper around the Javascript library THREE
  */
object THREE {

  @js.native
  trait Vector3 extends js.Object {
    // technically, these could be `var`s but I like immutable data
    val x: Double
    val y: Double
    val z: Double

    def copy(v: Vector3): Unit

    def add(v: Vector3): Unit

    def length(): Double
  }

  object Vector3 {
    def apply(x: Double, y: Double, z: Double): Vector3 =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Vector3)(x, y, z).asInstanceOf[Vector3]

    def apply(x: Double): Vector3 = apply(x, x, x)

    def unapply(v: Vector3): Option[(Double, Double, Double)] =
      Some((v.x, v.y, v.z))

    def clone(v: Vector3) = Vector3(v.x, v.y, v.z)
  }

  @js.native
  trait Scene extends js.Object {
    def add(so: SceneObject): Unit
  }

  object Scene {
    def apply(): Scene =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Scene)().asInstanceOf[Scene]
  }

  @js.native
  trait Camera extends js.Object {
    val target: Vector3
  }

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
  trait Geometry extends js.Object {
    val vertices: js.Array[Vector3]
    var verticesNeedUpdate: Boolean
  }

  object Geometry {
    def apply(): Geometry =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Geometry)().asInstanceOf[Geometry]
  }

  @js.native
  trait BoxGeometry extends Geometry

  object BoxGeometry {
    def apply(x: Double, y: Double, z: Double): BoxGeometry = {
      assert(x > 0)
      assert(y > 0)
      assert(z > 0)
      js.Dynamic.newInstance(js.Dynamic.global.THREE.BoxGeometry)(
        x, y, z
      ).asInstanceOf[BoxGeometry]
    }

    def apply(size: Double): BoxGeometry = {
      apply(size, size, size)
    }
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
  trait SphereGeometry extends Geometry

  object SphereGeometry {
    def apply(radius: Double, widthSegments: Int, heightSegments: Int): SphereGeometry = {
      assert(radius > 0)
      assert(widthSegments > 0)
      assert(heightSegments > 0)
      js.Dynamic.newInstance(js.Dynamic.global.THREE.SphereGeometry)(
        radius, widthSegments, heightSegments
      ).asInstanceOf[SphereGeometry]
    }
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
  trait MeshBasicMaterial extends Material

  object MeshBasicMaterial {
    def apply(attrs: js.Object): MeshBasicMaterial =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.MeshBasicMaterial)(
        attrs
      ).asInstanceOf[MeshBasicMaterial]
  }

  @js.native
  trait SceneObject extends js.Object {
    var position: Vector3
    var geometry: Geometry
  }

  @js.native
  trait Line extends SceneObject

  object Line {
    def apply(geometry: Geometry, material: Material): Line =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Line)(
        geometry, material
      ).asInstanceOf[Line]
  }

  @js.native
  trait LineSegments extends SceneObject

  object LineSegments {
    def apply(geometry: Geometry, material: Material): LineSegments =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.LineSegments)(
        geometry, material
      ).asInstanceOf[LineSegments]
  }

  @js.native
  trait Mesh extends SceneObject

  object Mesh {
    def apply(geometry: Geometry, material: Material): Mesh =
      js.Dynamic.newInstance(js.Dynamic.global.THREE.Mesh)(
        geometry, material
      ).asInstanceOf[Mesh]
  }

}
