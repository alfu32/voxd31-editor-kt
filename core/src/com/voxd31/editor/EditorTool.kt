package com.voxd31.editor

import com.badlogic.gdx.math.Vector3

class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Event) -> Boolean,
    var onMove: (self: EditorTool,event: Event) -> Boolean,
) {
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
