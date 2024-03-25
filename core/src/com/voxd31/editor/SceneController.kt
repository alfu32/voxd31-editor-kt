package com.voxd31.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray

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


fun findIntersection(ray: Ray, voxels: List<Cube>,modelBuilder:ModelBuilder): ModelIntersection {
    val VOX_SZ2 = Vector3(0.5f,0.5f,0.5f)
    var closestIntersection: Vector3? = null
    var intersectionNormal: Vector3? = null
    var minDistance = Float.MAX_VALUE

    var target:Cube? = null
    voxels.forEach{ cube ->
        val bounds = BoundingBox(
            cube.position.sub(VOX_SZ2),
            cube.position.add(VOX_SZ2),
        )
        val intersection = Vector3()
        if(Intersector.intersectRayBounds(ray,bounds,intersection)){
            val distance = ray.origin.dst2(intersection)
            if(distance < minDistance) {
                minDistance = distance
                closestIntersection = intersection
                target = cube

                intersectionNormal = Vector3 (
                    if ( (intersection.x < bounds.max.x) && (intersection.x > bounds.min.x )) 1f else 0f,
                    if ( (intersection.y < bounds.max.y) && (intersection.y > bounds.min.y )) 1f else 0f,
                    if ( (intersection.z < bounds.max.z) && (intersection.z > bounds.min.z )) 1f else 0f,
                ).nor()
            }
        }
    }

    return if ( closestIntersection != null && intersectionNormal != null ) {
        ModelIntersection(true,closestIntersection!!,intersectionNormal!!,target!!,"cube")
    } else {
        ModelIntersection(false, target = Cube(modelBuilder = ModelBuilder(),position = Vector3(0f,1f,0f), color = Color.CYAN))
    }
}


class SceneController(val modelBuilder: ModelBuilder, val camera: Camera) {
    var cubes: MutableList<Cube> = mutableListOf()
    var currentColor: Color = Color.RED
    fun rayToPointDistance(ray: Ray, point: Vector3): Float {
        // Vector from the ray's origin to the point
        val originToPoint = Vector3(point).sub(ray.origin)

        // Cross product of the direction of the ray and the vector from the ray's origin to the point
        val crossProduct = ray.direction.crs(originToPoint)

        // Distance formula: magnitude of the cross product divided by the magnitude of the ray's direction
        return crossProduct.len() / ray.direction.len()
    }
    public fun screenToModelPoint(screenX: Int, screenY: Int): ModelIntersection {
        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera.getPickRay(screenX.toFloat(), screenY.toFloat())

        val intersectionToScene = findIntersection(ray,cubes,modelBuilder)
        if ( intersectionToScene.hit ) {
            return intersectionToScene
        } else {
            val intersection = Vector3()
            // Assume a plane at y = 0 for the intersection, you might adjust this based on your scene
            Intersector.intersectRayPlane(ray, Plane(Vector3.Y, 0f), intersection)
            return ModelIntersection(
                hit = true,
                point = intersection,
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(0f,0f,0f), color = Color.CYAN),
                type = "ground",
            )
        }
    }

    fun addCube(x: Int, y: Int, z: Int,color:Color? = null) {
        val cube = createCubeAt(x, y, z,color)
        cubes.add(cube)
    }

    fun removeCube(x: Int, y: Int, z: Int) {
        // This method will remove the first cube found at the given coordinates
        // More sophisticated logic might be needed for your specific use case
        val iterator = cubes.iterator()
        while (iterator.hasNext()) {
            val cube = iterator.next()
            if (cube.position.epsilonEquals(x.toFloat(), y.toFloat(), z.toFloat(), 0.8f)) {
                iterator.remove()
                break
            }
        }
    }

    private fun createCubeAt(x: Int, y: Int, z: Int,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(x.toFloat(),y.toFloat(),z.toFloat()),if( color == null ) currentColor else color)
    }
    fun clear() {
        cubes = mutableListOf()
    }

    fun dispose() {
        cubes.forEach { it.instance.model.dispose() }
    }

    // Additional methods for scene management...
}