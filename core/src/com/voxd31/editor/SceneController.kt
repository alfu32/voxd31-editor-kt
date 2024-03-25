package com.voxd31.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray

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
    public fun screenToModelPoint(screenX: Int, screenY: Int): Vector3 {
        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera.getPickRay(screenX.toFloat(), screenY.toFloat())
        val intersection = Vector3()
        for (cube in cubes){
            /// if(rayToPointDistance(ray,cube.position)<0.630) {
            ///     return cube.position
            /// }
            for(mesh in cube.getModelInstance(modelBuilder).model.meshes){
                if(Intersector.intersectRayBounds(ray,mesh.calculateBoundingBox(),intersection)){
                    return intersection
                }
            }
        }
        // Assume a plane at y = 0 for the intersection, you might adjust this based on your scene
        Intersector.intersectRayPlane(ray, Plane(Vector3.Y, 0f), intersection)
        return intersection
    }

    fun addCube(x: Float, y: Float, z: Float) {
        val cube = createCubeAt(x, y, z)
        cubes.add(cube)
    }

    fun removeCube(x: Float, y: Float, z: Float) {
        // This method will remove the first cube found at the given coordinates
        // More sophisticated logic might be needed for your specific use case
        val iterator = cubes.iterator()
        while (iterator.hasNext()) {
            val cube = iterator.next()
            if (cube.position.epsilonEquals(x, y, z, 0.8f)) {
                iterator.remove()
                break
            }
        }
    }

    private fun createCubeAt(x: Float, y: Float, z: Float): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(x,y,z),currentColor)
    }
    fun clear() {
        cubes = mutableListOf()
    }

    fun dispose() {
        cubes.forEach { it.instance.model.dispose() }
    }

    // Additional methods for scene management...
}