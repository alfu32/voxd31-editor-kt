package com.voxd31.editor

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3

abstract class EditorTool(
    open var name: String,
    open var scene: SceneController,
    open var camera: Camera,
) {
    abstract fun onFinished(sceneController: SceneController,point: Vector3)//(callback: (scene: SceneController,point: Vector3) -> Unit)
    abstract fun onProgress(sceneController: SceneController,point: Vector3)//(callback: (scene: SceneController,point: Vector3) -> Unit)

}
