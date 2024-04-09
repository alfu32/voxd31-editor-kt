package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Event) -> Boolean,
    var onMove: (self: EditorTool,event: Event) -> Boolean,
) {
    companion object {
        val VoxelEditor = { scene: SceneController, feedback: SceneController ->
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            EditorTool(
                name = "voxel",
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    if (event.keyDown != Input.Keys.CONTROL_LEFT && event.keyDown != Input.Keys.SHIFT_LEFT) {
                        if (event.keyDown == Input.Keys.ALT_LEFT) {
                            scene.removeCube(
                                event.target!!
                            )
                        } else {
                            scene.addCube(
                                event.modelNextVoxel!!
                            )
                        }
                    }
                    //currentEvent = event
                    return true
                },
                onMove = fun(self: EditorTool, event: Event): Boolean {
                    feedback.clear()
                    feedback.addCube(event.modelVoxel!!, a)
                    feedback.addCube(event.modelNextVoxel!!, b)
                    //currentEvent = event
                    return true
                }
            )
        }
    }
    var points:MutableList<Vector3> = mutableListOf()
    fun handleEvent(event:Event) {
        val newPoints= points + event.modelPoint!!
        val isFinished  = onClick(this, event)
        if(isFinished) {
            points = mutableListOf()
        } else {
            points = newPoints.toMutableList()
        }
    }

}
