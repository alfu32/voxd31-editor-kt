package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import kotlin.math.floor

class ModelIntersection(
    var hit : Boolean,
    var point: Vector3 = Vector3(0f,0f,0f),
    var normal: Vector3 = Vector3(0f,0f,0f),
    var target: Cube,// = Cube(modelBuilder = ModelBuilder(),position = Vector3(0f,0f,0f), color = Color.CYAN),
    val type: String = "undefined"
) {
    operator fun component1(): Boolean {
        return hit
    }
    operator fun component2(): Vector3 {
        return point
    }
    operator fun component3(): Vector3 {
        return normal
    }
    operator fun component4(): Cube {
        return target
    }
    operator fun component5(): String {
        return type
    }
}


class SceneController(val modelBuilder: ModelBuilder) {
    var cubes: HashMap<String,Cube> = hashMapOf()
    var currentColor: Color = Color.RED
    fun sceneIntersectRay(ray: Ray): ModelIntersection {
        val intersections = cubes.map { (id, cube) -> cube.intersectsRay(ray) }.filterNotNull().filter{ it.hit }
        if (intersections.isEmpty()) {
            val intersection = Vector3()
            // Assume a plane at y = 0 for the intersection, you might adjust this based on your scene
            Intersector.intersectRayPlane(ray, Plane(Vector3.Y, 0f), intersection)
            return ModelIntersection(
                hit = true,
                point = intersection,
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(intersection.x,intersection.y,intersection.z), color = Color.CYAN),
                type = "ground",
            )
        } else {
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(ray.origin) }!!
        }
    }
    fun addCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        cubes[cube.getId()]=cube
        /// println("cubes : ${cubes.size}")
    }
    fun removeCube(c: Cube) {
        cubes.remove(c.getId())
    }
    fun createCubeAt(p:Vector3,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(floor(p.x),floor(p.y),floor(p.z)), color ?: currentColor)
    }
    fun clear() {
        cubes = hashMapOf()
    }

    fun dispose() {
        if(cubes.size > 0)cubes.values.first().getModelInstance().model.dispose()
        /// cubes.forEach { it.instance.model.dispose() }
    }

    fun removeCube(v: Vector3) {
        val c=createCubeAt(v)
        removeCube(c)
    }

    // Additional methods for scene management...
}