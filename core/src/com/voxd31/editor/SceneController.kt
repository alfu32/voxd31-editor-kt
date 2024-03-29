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
            cube.position.cpy().sub(VXSZ),
            cube.position.cpy().add(VXSZ),
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
    fun screenToModelPoint(screenX: Int, screenY: Int): ModelIntersection {
        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera.getPickRay(
            screenX.toFloat(), screenY.toFloat(),
            screenX.toFloat()/camera.viewportWidth, screenY.toFloat()/camera.viewportHeight,
            camera.viewportWidth,camera.viewportHeight,
        )
        val intersections = cubes.map { (id, cube) ->
            if (Intersector.intersectRayBoundsFast(ray, cube.position.cpy().sub(0.5f), Vector3(1f, 1f, 1f))) {
                var intersections = hashMapOf(
                    "top" to Pair(Vector3(), Float.MAX_VALUE),
                    "bottom" to Pair(Vector3(), Float.MAX_VALUE),
                    "left" to Pair(Vector3(), Float.MAX_VALUE),
                    "right" to Pair(Vector3(), Float.MAX_VALUE),
                    "back" to Pair(Vector3(), Float.MAX_VALUE),
                    "front" to Pair(Vector3(), Float.MAX_VALUE),
                )
                var normals = hashMapOf(
                    "top" to Vector3(0f, 1f, 0f),
                    "bottom" to Vector3(0f, -1f, 0f),
                    "left" to Vector3(-1f, 0f, 0f),
                    "right" to Vector3(1f, 0f, 0f),
                    "back" to Vector3(0f, 0f, -1f),
                    "front" to Vector3(0f, 0f, 1f),
                )
                val trMap = cube.getFaceTriangles()
                var intersects = hashMapOf<String, Boolean>()
                intersects["top"] = Intersector.intersectRayTriangles(ray, trMap["top"], intersections["top"]!!.first)
                intersects["bottom"] =
                    Intersector.intersectRayTriangles(ray, trMap["bottom"], intersections["bottom"]!!.first)
                intersects["left"] =
                    Intersector.intersectRayTriangles(ray, trMap["left"], intersections["left"]!!.first)
                intersects["right"] =
                    Intersector.intersectRayTriangles(ray, trMap["right"], intersections["right"]!!.first)
                intersects["back"] =
                    Intersector.intersectRayTriangles(ray, trMap["back"], intersections["back"]!!.first)
                intersects["front"] =
                    Intersector.intersectRayTriangles(ray, trMap["front"], intersections["front"]!!.first)
                intersections["top"] =
                    Pair(intersections["top"]!!.first, intersections["top"]!!.first.cpy().dst(ray.origin))
                intersections["bottom"] =
                    Pair(intersections["bottom"]!!.first, intersections["bottom"]!!.first.cpy().dst(ray.origin))
                intersections["left"] =
                    Pair(intersections["left"]!!.first, intersections["left"]!!.first.cpy().dst(ray.origin))
                intersections["right"] =
                    Pair(intersections["right"]!!.first, intersections["right"]!!.first.cpy().dst(ray.origin))
                intersections["back"] =
                    Pair(intersections["back"]!!.first, intersections["back"]!!.first.cpy().dst(ray.origin))
                intersections["front"] =
                    Pair(intersections["front"]!!.first, intersections["front"]!!.first.cpy().dst(ray.origin))

                var min_val = Float.MAX_VALUE
                var face_index = "not_found"
                for ((name, pair) in intersections) {
                    if (intersects[name] == true) {
                        if (pair.second <= min_val) {
                            face_index = name
                            min_val = pair.second
                        }
                    }
                }
                if (face_index != "not_found") {
                    ModelIntersection(
                        hit = true,
                        point = intersections[face_index]!!.first,
                        target = cube,
                        normal = normals[face_index]!!,
                        type = face_index
                    )
                } else null

            } else null
        }.filterNotNull().filter{it.hit}
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
        }else{
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(camera.position) }!!
        }
    }

    fun addCube(x: Int, y: Int, z: Int,color:Color? = null) {
        val cube = createCubeAt(x, y, z,color)
        cubes[cube.getId()]=cube
    }
    fun addCube(x: Float, y: Float, z: Float,color:Color? = null) {
        val cube = createCubeAt(x, y, z,color)
        cubes[cube.getId()]=cube
        println("cubes : ${cubes.size}")
    }
    fun addCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        cubes[cube.getId()]=cube
        println("cubes : ${cubes.size}")
    }

    fun removeCube(x: Int, y: Int, z: Int) {
        val cube = createCubeAt(x, y, z,Color.RED)
        cubes.remove(cube.getId())
    }
    fun removeCube(x: Float, y: Float, z: Float) {
        val cube = createCubeAt(x, y, z,Color.RED)
        cubes.remove(cube.getId())
    }
    fun removeCube(c: Cube) {
        cubes.remove(c.getId())
    }

    private fun createCubeAt(x: Int, y: Int, z: Int,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(x.toFloat(),y.toFloat(),z.toFloat()),if( color == null ) currentColor else color)
    }
    private fun createCubeAt(x: Float, y: Float, z: Float,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(x,y,z),if( color == null ) currentColor else color)
    }
    private fun createCubeAt(p:Vector3,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = p,if( color == null ) currentColor else color)
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