package com.voxd31.gdxui

import com.badlogic.gdx.math.Vector3


class ModelIntersection(
    var hit : Boolean,
    var point: Vector3 = Vector3(0f,0f,0f),
    var normal: Vector3 = Vector3(0f,0f,0f),
    var target: Cube,// = Cube(modelBuilder = ModelBuilder(),position = Vector3(0f,0f,0f), color = Color.CYAN),
    var type: String = "undefined"
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

    fun copy(): ModelIntersection  = ModelIntersection(
        hit = hit,
        point = point.cpy(),
        normal = normal.cpy(),
        target = target.copy(),
        type=type,
    )
}