package com.voxd31.editor

import com.badlogic.gdx.math.MathUtils.floor
import com.badlogic.gdx.math.Vector3

class Vector3i(
    var x:Int,
    var y:Int,
    var z:Int,
) {
    fun cpy(): Vector3i = Vector3i(x,y,z)
    fun add(x: Float, y: Float, z: Float): Vector3 {
        return Vector3(this.x+x,this.y+y,this.z+z)
    }

    fun sub(x: Float, y: Float, z: Float): Vector3 {
        return Vector3(this.x-x,this.y-y,this.z-z)
    }

    fun vec3(): Vector3 {
        return Vector3(this.x+0.0f,this.y+0.0f,this.z+0.0f)
    }

    companion object {
        fun fromFloats(x: Float, y: Float, z: Float): Vector3i {
            return Vector3i(floor(x),floor(y),floor(z))
        }
    }
}