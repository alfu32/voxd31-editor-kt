package com.voxd31.editor

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3

class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Event) -> Boolean,
    var onMove: (self: EditorTool,event: Event) -> Boolean,
) {
    var points:MutableList<Vector3> = mutableListOf()
    fun takeEvent(event:Event) {
        val newPoints= points + event.model!!
        val isFinished  = onClick(this, event)
        if(isFinished) {
            points = mutableListOf()
        } else {
            points = newPoints.toMutableList()
        }
    }

}
