package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
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
        fun SelectEditor(
            scene: SceneController,
            feedback: SceneController,
            selected: SceneController,
            queryWindowSelection: (startRaw: Vector2, endRaw: Vector2) -> List<com.voxd31.gdxui.Cube>
        ):EditorTool{
            val hoverColor = Color(1f,1f,0f,0.5f)
            val nextColor = Color(1f,0.5f,0f,0.5f)
            val volumePreviewColor = Color(0.15f, 0.8f, 1f, 0.35f)
            val windowColor = Color(0.15f, 0.8f, 1f, 0.9f)
            val dragStartStage = Vector2()
            val dragCurrentStage = Vector2()
            val dragStartRaw = Vector2()
            val dragCurrentRaw = Vector2()
            val dragThresholdSq = 49f
            val doubleClickThresholdMs = 350L
            var pointerDown = false
            var pointerDownOnGround = false
            var draggingWindow = false
            var volumeSelectionStart: Vector3? = null
            var lastClickedSceneCubeId: String? = null
            var lastClickAtMs = 0L
            var lastClickWasSelectedBeforeAction = false

            fun isSceneCube(event: Vox3Event): com.voxd31.gdxui.Cube? {
                val target = event.target ?: return null
                return scene.cubes[target.getId()]
            }

            fun updateHoverFeedback(event: Vox3Event) {
                if (draggingWindow) {
                    feedback.clear()
                    return
                }
                val volumeStart = volumeSelectionStart
                if (volumeStart != null) {
                    val current = event.modelVoxel?.cpy() ?: event.modelNextVoxel?.cpy()
                    if (current != null) {
                        feedback.clear()
                        val minX = minOf(volumeStart.x, current.x)
                        val minY = minOf(volumeStart.y, current.y)
                        val minZ = minOf(volumeStart.z, current.z)
                        val maxX = maxOf(volumeStart.x, current.x)
                        val maxY = maxOf(volumeStart.y, current.y)
                        val maxZ = maxOf(volumeStart.z, current.z)
                        scene.cubes.values.forEach { cube ->
                            val p = cube.position
                            if (p.x in minX..maxX && p.y in minY..maxY && p.z in minZ..maxZ) {
                                feedback.addCube(p, volumePreviewColor)
                            }
                        }
                    }
                    return
                }
                feedback.clear()
                event.modelVoxel?.let { feedback.addCube(it, hoverColor) }
                event.modelNextVoxel?.let { feedback.addCube(it, nextColor) }
            }

            fun applyVolumeSelection(a: Vector3, b: Vector3) {
                selected.clear()
                val minX = minOf(a.x, b.x)
                val minY = minOf(a.y, b.y)
                val minZ = minOf(a.z, b.z)
                val maxX = maxOf(a.x, b.x)
                val maxY = maxOf(a.y, b.y)
                val maxZ = maxOf(a.z, b.z)
                scene.cubes.values.forEach { cube ->
                    val p = cube.position
                    if (p.x in minX..maxX && p.y in minY..maxY && p.z in minZ..maxZ) {
                        selected.addCube(p, cube.color)
                    }
                }
            }

            fun contiguousRegion(start: com.voxd31.gdxui.Cube): List<com.voxd31.gdxui.Cube> {
                val colorKey = Color.rgba8888(start.color)
                val queue = ArrayDeque<com.voxd31.gdxui.Cube>()
                val visited = linkedSetOf<String>()
                val region = mutableListOf<com.voxd31.gdxui.Cube>()
                queue.add(start)
                while (queue.isNotEmpty()) {
                    val cube = queue.removeFirst()
                    if (!visited.add(cube.getId())) {
                        continue
                    }
                    region += cube
                    val x = cube.position.x.toInt()
                    val y = cube.position.y.toInt()
                    val z = cube.position.z.toInt()
                    listOf(
                        Triple(x - 1, y, z),
                        Triple(x + 1, y, z),
                        Triple(x, y - 1, z),
                        Triple(x, y + 1, z),
                        Triple(x, y, z - 1),
                        Triple(x, y, z + 1)
                    ).forEach { (nx, ny, nz) ->
                        val neighbor = scene.cubesInt["{$nx,$ny,$nz}"] ?: return@forEach
                        if (Color.rgba8888(neighbor.color) == colorKey && neighbor.getId() !in visited) {
                            queue.add(neighbor)
                        }
                    }
                }
                return region
            }

            return object:EditorTool(
                name = "Select2",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
                    updateHoverFeedback(event)
                    return true
                }
            ){
                override fun touchDown(event: Vox3Event) {
                    if (event.button != Input.Buttons.LEFT) {
                        return
                    }
                    pointerDown = true
                    pointerDownOnGround = isSceneCube(event) == null
                    draggingWindow = false
                    event.screen?.let {
                        dragStartStage.set(it)
                        dragCurrentStage.set(it)
                    }
                    event.screenRaw?.let {
                        dragStartRaw.set(it)
                        dragCurrentRaw.set(it)
                    }
                }

                override fun touchDragged(event: Vox3Event) {
                    if (!pointerDown || !pointerDownOnGround) {
                        return
                    }
                    event.screen?.let { dragCurrentStage.set(it) }
                    event.screenRaw?.let { dragCurrentRaw.set(it) }
                    val dx = dragCurrentRaw.x - dragStartRaw.x
                    val dy = dragCurrentRaw.y - dragStartRaw.y
                    if (dx * dx + dy * dy >= dragThresholdSq) {
                        draggingWindow = true
                        volumeSelectionStart = null
                        feedback.clear()
                    }
                }

                override fun touchUp(event: Vox3Event) {
                    if (event.button != Input.Buttons.LEFT) {
                        return
                    }
                    val sceneCube = isSceneCube(event)
                    if (draggingWindow) {
                        selected.clear()
                        queryWindowSelection(dragStartRaw, dragCurrentRaw).forEach { cube ->
                            selected.addCube(cube.position, cube.color)
                        }
                        feedback.clear()
                        pointerDown = false
                        draggingWindow = false
                        return
                    }

                    if (sceneCube != null) {
                        volumeSelectionStart = null
                        feedback.clear()
                        val now = System.currentTimeMillis()
                        val isDoubleClick =
                            lastClickedSceneCubeId == sceneCube.getId() &&
                                now - lastClickAtMs <= doubleClickThresholdMs

                        if (isDoubleClick) {
                            val region = contiguousRegion(sceneCube)
                            if (lastClickWasSelectedBeforeAction) {
                                region.forEach { selected.removeCube(it) }
                            } else {
                                region.forEach { selected.addCube(it.position, it.color) }
                            }
                            lastClickedSceneCubeId = null
                            lastClickAtMs = 0L
                        } else {
                            val wasSelected = selected.cubes.containsKey(sceneCube.getId())
                            if (wasSelected) {
                                selected.removeCube(sceneCube)
                            } else {
                                selected.addCube(sceneCube.position, sceneCube.color)
                            }
                            lastClickedSceneCubeId = sceneCube.getId()
                            lastClickAtMs = now
                            lastClickWasSelectedBeforeAction = wasSelected
                        }

                        pointerDown = false
                        return
                    }

                    val releasePoint = event.modelVoxel?.cpy() ?: event.modelNextVoxel?.cpy()
                    if (pointerDownOnGround && releasePoint != null) {
                        val start = volumeSelectionStart
                        if (start == null) {
                            volumeSelectionStart = releasePoint
                            updateHoverFeedback(event)
                        } else {
                            applyVolumeSelection(start, releasePoint)
                            volumeSelectionStart = null
                            feedback.clear()
                        }
                    } else {
                        volumeSelectionStart = null
                        feedback.clear()
                    }
                    pointerDown = false
                }

                override fun drawScreenOverlay(shapeRenderer: ShapeRenderer) {
                    if (!draggingWindow) {
                        return
                    }
                    val minX = minOf(dragStartStage.x, dragCurrentStage.x)
                    val minY = minOf(dragStartStage.y, dragCurrentStage.y)
                    val width = kotlin.math.abs(dragCurrentStage.x - dragStartStage.x)
                    val height = kotlin.math.abs(dragCurrentStage.y - dragStartStage.y)
                    shapeRenderer.color = windowColor
                    shapeRenderer.rect(minX, minY, width, height)
                }

                override fun reset() {
                    pointerDown = false
                    pointerDownOnGround = false
                    draggingWindow = false
                    volumeSelectionStart = null
                    feedback.clear()
                }
            }
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
    open fun touchDown(event: Vox3Event) {}
    open fun touchDragged(event: Vox3Event) {}
    open fun touchUp(event: Vox3Event) {
        handleEvent(event)
    }
    open fun drawScreenOverlay(shapeRenderer: ShapeRenderer) {}

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
