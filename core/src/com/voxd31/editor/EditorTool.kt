package com.voxd31.editor

import com.badlogic.gdx.math.Vector3

interface EditorTool {
    val activationKey: Int
    val requiredPoints: Int
    val acquiredPoints: MutableList<Vector3>

    fun activate()
    fun deactivate()
    fun acquirePoint(point: Vector3)
    fun execute(scene: SceneController)
}