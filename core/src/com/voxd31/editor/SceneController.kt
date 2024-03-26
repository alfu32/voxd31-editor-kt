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


fun intersectRayWithCubes(ray: Ray, voxels: List<Cube>,modelBuilder:ModelBuilder): ModelIntersection {
    val VXSZ = 0.5F
    var closestIntersection: Vector3? = null
    var intersectionNormal: Vector3? = null
    var minDistance = Float.MAX_VALUE

    var target:Cube? = null
    voxels.forEach{ cube ->
        val bounds = BoundingBox(
            Vector3(cube.position.x,cube.position.y,cube.position.z),
            Vector3(cube.position.x + 1.0f,cube.position.y + 1.0f,cube.position.z + 1.0f),
        )
        //println("bounds: $bounds, ray: $ray")
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
    var cubes: HashMap<String,Cube> = hashMapOf()
    var currentColor: Color = Color.RED
    fun rayToPointDistance(ray: Ray, point: Vector3): Float {
        // Vector from the ray's origin to the point
        val originToPoint = Vector3(point).sub(ray.origin)

        // Cross product of the direction of the ray and the vector from the ray's origin to the point
        val crossProduct = ray.direction.crs(originToPoint)

        // Distance formula: magnitude of the cross product divided by the magnitude of the ray's direction
        return crossProduct.len() / ray.direction.len()
    }
    fun intersectRayWithCube(ray: Ray, cube:Cube): ModelIntersection {
        val cubeCenter = cube.position
        val cubeSize = 1f
        val halfSize = cubeSize / 2
        val min = cubeCenter.cpy().sub(halfSize, halfSize, halfSize)
        val max = cubeCenter.cpy().add(halfSize, halfSize, halfSize)

        // Planes of the cube
        val planes = arrayOf(
            Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f), // +x, -x
            Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f), // +y, -y
            Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f)  // +z, -z
        )
        val distances = FloatArray(planes.size) { Float.MAX_VALUE }

        for ((index, plane) in planes.withIndex()) {
            val denom = plane.dot(ray.direction)
            if (denom != 0f) {
                val t = (plane.dot(cubeCenter) - plane.dot(ray.origin)) / denom
                if (t > 0) {
                    // Potential intersection point
                    val intersection = ray.origin.cpy().add(ray.direction.cpy().scl(t))
                    // Check if intersection is within cube bounds
                    if (intersection.x in min.x..max.x && intersection.y in min.y..max.y && intersection.z in min.z..max.z) {
                        distances[index] = t
                    }
                }
            }
        }

        val minDistanceIndex = distances.indices.minByOrNull { distances[it] }
        if (minDistanceIndex == null || distances[minDistanceIndex] == Float.MAX_VALUE) {
            return ModelIntersection(
                hit=false,
                point=Vector3(),
                normal=Vector3(),
                target=cube,
                type="undefined"
            ) // No intersection
        }

        val intersectionPoint = ray.origin.cpy().add(ray.direction.cpy().scl(distances[minDistanceIndex]))
        val normal = planes[minDistanceIndex]

        return ModelIntersection(
            hit=true,
            point=intersectionPoint,
            normal=normal,
            target=cube,
            type="cube"
        )
        // return Pair(intersectionPoint, normal)
    }
    public fun screenToModelPoint(screenX: Int, screenY: Int): ModelIntersection {
        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera.getPickRay(screenX.toFloat(), screenY.toFloat())

        // for (cube in cubes) {
        //     val intersectionToScene = intersectRayWithCube(ray,cube)
        //     if ( intersectionToScene.hit ) {
        //         return intersectionToScene
        //     }
        // }
        val intersectionToScene = intersectRayWithCubes(ray,cubes.values.toList(),modelBuilder)
        if ( intersectionToScene.hit) {
            return intersectionToScene
        }
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
    }

    fun addCube(x: Int, y: Int, z: Int,color:Color? = null) {
        val cube = createCubeAt(x, y, z,color)
        cubes[cube.getId()]=cube
    }
    fun addCube(x: Float, y: Float, z: Float,color:Color? = null) {
        val cube = createCubeAt(x.toInt(), y.toInt(), z.toInt(),color)
        cubes[cube.getId()]=cube
        println("cubes : ${cubes.size}")
    }

    fun removeCube(x: Int, y: Int, z: Int) {
        val cube = createCubeAt(x, y, z,Color.RED)
        cubes.remove(cube.getId())
    }
    fun removeCube(x: Float, y: Float, z: Float) {
        val cube = createCubeAt(x.toInt(), y.toInt(), z.toInt(),Color.RED)
        cubes.remove(cube.getId())
    }

    private fun createCubeAt(x: Int, y: Int, z: Int,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(x.toFloat(),y.toFloat(),z.toFloat()),if( color == null ) currentColor else color)
    }
    fun clear() {
        cubes = hashMapOf()
    }

    fun dispose() {
        if(cubes.size > 0)cubes.values.first().instance.model.dispose()
        /// cubes.forEach { it.instance.model.dispose() }
    }

    // Additional methods for scene management...
}