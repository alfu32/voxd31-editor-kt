package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.Vox3Event
import kotlin.math.atan2

open class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Vox3Event) -> Boolean,
    var onMove: (self: EditorTool,event: Vox3Event) -> Boolean,
) {
    public var commands= mutableListOf<String>()
    companion object {
        fun VoxelEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            return object:EditorTool(
                name = "voxel",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
                    feedback.clear()
                    feedback.addCube(event.modelVoxel!!, a)
                    feedback.addCube(event.modelNextVoxel!!,  b)
                    //currentEvent = event
                    return true
                }
            ){}
        }
        fun SelectEditor(scene: SceneController, feedback: SceneController, selected: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            fun crawl(){}
            return object:EditorTool(
                name = "Select2",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if(event.target != null ) {
                        val targetCube=event.target!!//selected.cubes.values.first()
                        if(scene.cubes[targetCube.getId()] != null) {
                            val alreadySelected = selected.cubes.containsKey(targetCube.getId())
                            selected.addCube(targetCube.position, targetCube.color)
                            val selectedCubesNeighbors=SceneController(targetCube.modelBuilder)
                            var oldLen=selected.cubes.size
                            selectedCubesNeighbors.addCube(targetCube.position, targetCube.color)
                            while(true){
                                selectedCubesNeighbors.cubes.values
                                    .flatMap { c ->
                                        c.getNeighbouringPositions()
                                    }
                                    .filter { neighbouringPosition ->
                                        val snc = scene.cubesInt[neighbouringPosition.getIntId()]
                                        snc != null && snc.color == targetCube.color
                                    }
                                    .forEach {
                                        selectedCubesNeighbors.addCube(it.position, it.color)
                                    }
                                if(alreadySelected){
                                    println("removing connected from selection")
                                    selectedCubesNeighbors.cubes.forEach { (k, v) ->
                                        selected.removeCube(v)
                                    }
                                } else {
                                    println("adding connected from selection")
                                    selectedCubesNeighbors.cubes.forEach { (k, v) ->
                                        selected.addCube(
                                            v.position,
                                            v.color
                                        )
                                    }
                                }
                                // selectedCubesNeighbors.clear()
                                if(oldLen==selected.cubes.size){
                                    break
                                }
                                oldLen=selected.cubes.size
                            }
                        } else {
                            selected.clear()
                        }
                    } else {
                        selected.clear()
                    }
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
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
        fun ArcEditor(scene: SceneController, feedback: SceneController):EditorTool{
            var voxelGen:( (p1: Vector3, p2: Vector3, p3: Vector3, callback: (p: Vector3) -> Unit) -> Unit)? = null
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            val points= mutableListOf(Vector3(),Vector3(),Vector3())
            var state=0
            return object:EditorTool(
                name = "Arc",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
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
                                    voxelGen!!(points[0],points[1],points[2]){
                                        scene.removeCube(it)
                                    }
                                } else {
                                    val a=Vector3i.fromFloats(points[0].x,points[0].y,points[0].z)
                                    val b=Vector3i.fromFloats(points[1].x,points[1].y,points[1].z)
                                    val c=Vector3i.fromFloats(points[2].x,points[2].y,points[2].z)
                                    self.commands = mutableListOf(
                                        "# Plane ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${c.x} ${c.y} ${c.z}  ${scene.currentColor}",
                                    )
                                    voxelGen!!(points[0],points[1],points[2]){ p ->
                                        scene.addCube(p)
                                        val a=Vector3i.fromFloats(p.x,p.y,p.z)
                                        self.commands.add("/setblock ${a.x} ${a.y} ${a.z} minecraft:stone")
                                    }
                                }
                                voxelGen=null
                                state=0
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
                    feedback.clear()
                    when(state){
                        0 ->{
                            feedback.addCube(event.modelVoxel!!, a)
                            feedback.addCube(event.modelNextVoxel!!, scene.currentColor)
                        }
                        1->{
                            if(voxelGen == null) {
                                val dir = atan2(points[0].z.toDouble(),points[0].x.toDouble())
                                voxelGen = voxelRangeArcGenerator(if(dir<0)-1 else 1)
                            }
                            voxelRangeSegment(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, Color.YELLOW)
                            }
                            voxelRangeSegment(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, Color.YELLOW)
                            }
                            feedback.addCube(points[0], Color.BLUE)
                            feedback.addCube(event.modelNextVoxel!!, Color.RED)
                        }
                        2->{
                            voxelGen!!(points[0],points[1],event.modelNextVoxel!!){
                                feedback.addCube(it, scene.currentColor)
                            }
                            voxelRangeSegment(points[0],points[1]){
                                feedback.addCube(it, Color.YELLOW)
                            }
                            voxelRangeSegment(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, Color.YELLOW)
                            }
                            feedback.addCube(points[0], Color.BLUE)
                            feedback.addCube(points[1], Color.RED)
                            feedback.addCube(event.modelNextVoxel!!, Color.GREEN)
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
    fun handleEvent(event:Vox3Event) {
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
