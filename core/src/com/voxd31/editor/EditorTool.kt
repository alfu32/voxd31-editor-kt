package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3

open class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Event) -> Boolean,
    var onMove: (self: EditorTool,event: Event) -> Boolean,
) {
    public var commands= mutableListOf<String>()
    companion object {
        fun VoxelEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            return object:EditorTool(
                name = "voxel",
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    if (event.keyDown != Input.Keys.CONTROL_LEFT && event.keyDown != Input.Keys.SHIFT_LEFT) {
                        val a=Vector3i.fromFloats(event.target!!.position.x,event.target!!.position.y,event.target!!.position.z)
                        if (event.keyDown == Input.Keys.ALT_LEFT) {
                            scene.removeCube(
                                event.target!!
                            )
                            self.commands.add("/setblock ${a.x} ${a.y} ${a.z} air replace")
                        } else {
                            scene.addCube(
                                event.modelNextVoxel!!
                            )
                            self.commands.add("/setblock ${a.x} ${a.y} ${a.z} minecraft:stone")
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
        fun VoidEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            return object:EditorTool(
                name = "nothing",
                onClick = fun(self: EditorTool, event: Event): Boolean {
                    return true
                },
                onMove = fun(self: EditorTool, event: Event): Boolean {
                    feedback.clear()
                    feedback.addCube(event.modelVoxel!!, a)
                    feedback.addCube(event.modelNextVoxel!!,  b)
                    //currentEvent = event
                    return true
                }
            ){}
        }
        fun makeTwoInputEditor(
            name:String,
            onFeedback: (a:Vector3,b:Vector3)->Unit,
            onEnd: (a:Vector3,b:Vector3)->List<String>,
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
                                self.commands.addAll(onEnd(points[0],points[1]))
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
            rasterizer: (a:Vector3,b:Vector3,op:(v:Vector3)->Unit)->List<String>
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
                                    val cmds = rasterizer(points[0],points[1]){
                                        scene.addCube(it)
                                    }
                                    self.commands.addAll(cmds)
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
        fun makeThreeInputEditor(
            name:String,
            onFeedback: (a:Vector3,b:Vector3,c:Vector3)->Unit,
            onEnd: (a:Vector3,b:Vector3,c:Vector3)->List<String>,
        ):EditorTool {
            val points= mutableListOf(Vector3(),Vector3(),Vector3())
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
                                onEnd(points[0],points[1],points[1])
                                state=2
                            }
                            2 -> {
                                points[2]=event.modelNextVoxel!!.cpy()
                                val cmds = onEnd(points[0],points[1],points[2])
                                self.commands.addAll(cmds)
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
                            onFeedback(event.modelNextVoxel!!,event.modelNextVoxel!!,event.modelNextVoxel!!)
                        }
                        1->{
                            onFeedback(points[0],event.modelNextVoxel!!,event.modelNextVoxel!!)
                        }
                        2->{
                            onFeedback(points[0],points[1],event.modelNextVoxel!!)
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
                                    val a=Vector3i.fromFloats(points[0].x,points[0].y,points[0].z)
                                    val b=Vector3i.fromFloats(points[1].x,points[1].y,points[1].z)
                                    val c=Vector3i.fromFloats(points[2].x,points[2].y,points[2].z)
                                    self.commands = mutableListOf(
                                        "# Plane ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${c.x} ${c.y} ${c.z}  ${scene.currentColor}",
                                    )
                                    voxelRangePlane(points[0],points[1],points[2]){ p ->
                                        scene.addCube(p)
                                        val a=Vector3i.fromFloats(p.x,p.y,p.z)
                                        self.commands.add("/setblock ${a.x} ${a.y} ${a.z} minecraft:stone")
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
