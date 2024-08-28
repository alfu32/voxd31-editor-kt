package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.voxd31.gdxui.Cube
import com.voxd31.gdxui.ModelIntersection
import kotlin.math.floor



class SceneController(val modelBuilder: ModelBuilder) {
    var cubes: HashMap<String, Cube> = hashMapOf()
    var cubesInt: HashMap<String,Cube> = hashMapOf()
    var currentColor: Color = Color.RED

    /**
     * possible values :
     *  - addWithoutReplace
     *  - addOrReplace
     *  - replaceCube
     * default value is addOrReplace
     */
    var addMode = "addOrReplace"
    fun sceneIntersectCubesRay(ray: Ray): ModelIntersection {
        val intersections = cubes.map { (id, cube) -> cube.intersectsRay(ray) }.filterNotNull().filter{ it.hit }
        if (!intersections.isEmpty()) {
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(ray.origin) }!!
        } else {
            return ModelIntersection(
                hit = false,
                point = Vector3(),
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(), color = Color.CYAN),
                type = "origin",
            )
        }
    }

    fun sceneIntersectGuidesRay(ray: Ray): ModelIntersection {
        val intersections = cubes.map { (id, cube) -> cube.intersectsGuidesRay(ray) }.filterNotNull().filter{ it.hit }
        if (!intersections.isEmpty()) {
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(ray.origin) }!!
        } else {
            return ModelIntersection(
                hit = false,
                point = Vector3(),
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(), color = Color.CYAN),
                type = "origin",
            )
        }
    }
    fun addCube(position: Vector3,color:Color? = null) {
        when(addMode){
            "addWithoutReplace" ->{
                add(position,color)
            }
            "addOrReplace" ->{
                addOrReplaceCube(position,color)
            }
            "replaceCube" ->{
                replaceCube(position,color)
            }
        }
    }
    fun add(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        if(cubes[cube.getId()] == null ) {
            cubes[cube.getId()] = cube
        }/// println("cubes : ${cubes.size}")
    }
    fun addOrReplaceCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        cubes[cube.getId()]=cube
        cubesInt[cube.getIntId()]=cube
        /// println("cubes : ${cubes.size}")
    }
    fun replaceCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        if(cubes[cube.getId()] != null ) {
            cubes[cube.getId()] = cube
            cubesInt[cube.getIntId()]=cube
        }
        /// println("cubes : ${cubes.size}")
    }
    fun removeCube(c: Cube) {
        cubes.remove(c.getId())
        cubesInt.remove(c.getIntId())
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

    fun cubeAt(p: Vector3): Cube? {
        val c=createCubeAt(p)
        return cubes[c.getId()]
    }

    // Additional methods for scene management...
}