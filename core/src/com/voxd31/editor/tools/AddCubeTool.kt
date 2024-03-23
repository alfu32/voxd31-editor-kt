package com.voxd31.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector3
import com.voxd31.editor.EditorTool
import com.voxd31.editor.SceneController

class AddCubeTool : EditorTool {
    override val activationKey = Input.Keys.A // Example key
    override val requiredPoints = 1
    override val acquiredPoints = mutableListOf<Vector3>()

    override fun activate() {
        println("Add Cube Tool activated")
    }

    override fun deactivate() {
        println("Add Cube Tool deactivated")
    }

    override fun acquirePoint(point: Vector3) {
        acquiredPoints.add(point)
    }

    override fun onFinished(scene: SceneController) {
        // Assuming a method to create a cube ModelInstance at a given point
        val point = acquiredPoints.first()
        scene.addCube(point.x,point.y,point.z)
    }

    override fun onProgress(sceneController: SceneController) {
        TODO("Not yet implemented")
    }
}