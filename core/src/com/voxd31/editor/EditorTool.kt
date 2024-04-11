package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

open class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Event) -> Boolean,
    var onMove: (self: EditorTool,event: Event) -> Boolean,
) {
    companion object {
        fun VoxelEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            return object:EditorTool(
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
                    feedback.addCube(event.modelNextVoxel!!,  scene.currentColor)
                    //currentEvent = event
                    return true
                }
            ){}
        }
        fun makeTwoInputEditor(
            name:String,
            onFeedback: (a:Vector3,b:Vector3)->Unit,
            onEnd: (a:Vector3,b:Vector3)->Unit,
        ):EditorTool {
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            val points= mutableListOf(Vector3(),Vector3())
            var state=0
            return object:EditorTool(
                name = name,
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    if (event.keyDown != Input.Keys.CONTROL_LEFT && event.keyDown != Input.Keys.SHIFT_LEFT) {
                        when(state){
                            0 -> {
                                points[0]=event.modelNextVoxel!!.cpy()
                                state=1
                            }
                            1 -> {
                                points[1]=event.modelNextVoxel!!.cpy()
                                onEnd(points[0],points[1])
                                state=0
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                },
                onMove = fun(self: EditorTool, event: Event): Boolean {
                    when(state){
                        0 ->{
                            onFeedback(event.modelNextVoxel!!,event.modelNextVoxel!!)
                        }
                        1->{
                            onFeedback(points[0],event.modelNextVoxel!!)
                        }
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun reset() {
                    state=0
                }

            }
        }
        fun makeTwoInputEditor(
            name:String,
            scene: SceneController,
            feedback: SceneController,
            rasterizer: (a:Vector3,b:Vector3,op:(v:Vector3)->Unit)->Unit
        ):EditorTool {
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            val points= mutableListOf(Vector3(),Vector3())
            var state=0
            return object:EditorTool(
                name = name,
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    if (event.keyDown != Input.Keys.CONTROL_LEFT && event.keyDown != Input.Keys.SHIFT_LEFT) {
                        when(state){
                            0 -> {
                                points[0]=event.modelNextVoxel!!.cpy()
                                state=1
                            }
                            1 -> {
                                points[1]=event.modelNextVoxel!!.cpy()
                                if (event.keyDown == Input.Keys.ALT_LEFT) {
                                    rasterizer(points[0],points[1]){
                                        scene.removeCube(it)
                                    }
                                } else {
                                    rasterizer(points[0],points[1]){
                                        scene.addCube(it)
                                    }
                                }
                                state=0
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                },
                onMove = fun(self: EditorTool, event: Event): Boolean {
                    feedback.clear()
                    when(state){
                        0 ->{
                            feedback.addCube(event.modelVoxel!!, a)
                            feedback.addCube(event.modelNextVoxel!!,  scene.currentColor)
                        }
                        1->{
                            rasterizer(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, scene.currentColor)
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun reset() {
                    state=0
                }

            }
        }
        fun PlaneEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            val points= mutableListOf(Vector3(),Vector3(),Vector3())
            var state=0
            return object:EditorTool(
                name = "plane",
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    if (event.keyDown != Input.Keys.CONTROL_LEFT && event.keyDown != Input.Keys.SHIFT_LEFT) {
                        when(state){
                            0 -> {
                                points[0]=event.modelNextVoxel!!.cpy()
                                state=1
                            }
                            1 -> {
                                points[1]=event.modelNextVoxel!!.cpy()
                                state=2
                            }
                            2 -> {
                                points[2]=event.modelNextVoxel!!.cpy()
                                if (event.keyDown == Input.Keys.ALT_LEFT) {
                                    voxelRangePlane(points[0],points[1],points[2]){
                                        scene.removeCube(it)
                                    }
                                } else {
                                    voxelRangePlane(points[0],points[1],points[2]){
                                        scene.addCube(it)
                                    }
                                }
                                state=0
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                },
                onMove = fun(self: EditorTool, event: Event): Boolean {
                    feedback.clear()
                    when(state){
                        0 ->{
                            feedback.addCube(event.modelVoxel!!, a)
                            feedback.addCube(event.modelNextVoxel!!, scene.currentColor)
                        }
                        1->{
                            voxelRangeSegment(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, scene.currentColor)
                            }
                        }
                        2->{
                            voxelRangePlane(points[0],points[1],event.modelNextVoxel!!){
                                feedback.addCube(it, scene.currentColor)
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun reset() {
                    state=0
                }
            }
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

    open fun reset(){}
}
