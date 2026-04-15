package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.Vox3Event
import kotlin.math.atan2

class ToolOperator(
    val label: String,
    val action: () -> Unit,
    val active: () -> Boolean = { false },
    val enabled: () -> Boolean = { true }
)

open class EditorTool(
    var name: String,
    var onClick: (self: EditorTool,event: Vox3Event) -> Boolean,
    var onMove: (self: EditorTool,event: Vox3Event) -> Boolean,
) {
    public var commands= mutableListOf<String>()
    companion object {
        private enum class ApplyMode { ADD, REMOVE }
        private enum class PolySegmentMode(val label: String) {
            LINE("Line Segment"),
            ARC("Arc Segment"),
            CIRCLE("Circle Segment")
        }

        private fun acceptsToolClick(event: Vox3Event): Boolean {
            return !event.ctrl &&
                !event.shift &&
                event.keyDown != Input.Keys.CONTROL_LEFT &&
                event.keyDown != Input.Keys.CONTROL_RIGHT &&
                event.keyDown != Input.Keys.SHIFT_LEFT &&
                event.keyDown != Input.Keys.SHIFT_RIGHT
        }

        private fun isRemoveMode(event: Vox3Event, mode: ApplyMode): Boolean {
            return mode == ApplyMode.REMOVE ||
                event.alt ||
                event.keyDown == Input.Keys.ALT_LEFT ||
                event.keyDown == Input.Keys.ALT_RIGHT
        }

        fun VoxelEditor(scene: SceneController, feedback: SceneController):EditorTool{
            val a = Color(1f,1f,0f,0.5f)
            val removePreview = Color(1f, 0.1f, 0.1f, 0.55f)
            var mode = ApplyMode.ADD
            return object:EditorTool(
                name = "voxel",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if (acceptsToolClick(event)) {
                        val a=Vector3i.fromFloats(event.target!!.position.x,event.target!!.position.y,event.target!!.position.z)
                        if (isRemoveMode(event, mode)) {
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
                    if (isRemoveMode(event, mode)) {
                        feedback.addCube(event.modelVoxel!!, removePreview)
                    } else {
                        feedback.addCube(event.modelVoxel!!, a)
                        feedback.addCube(event.modelNextVoxel!!,  scene.currentColor)
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Add", { mode = ApplyMode.ADD }, { mode == ApplyMode.ADD }),
                    ToolOperator("Remove", { mode = ApplyMode.REMOVE }, { mode == ApplyMode.REMOVE }),
                    ToolOperator("Reset", { reset() })
                )

                override fun reset() {
                    mode = ApplyMode.ADD
                    feedback.clear()
                }
            }
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
            val volumePreviewColor = Color(0.15f, 0.8f, 1f, 0.65f)
            val windowFillColor = Color(0.15f, 0.8f, 1f, 0.65f)
            val windowOutlineColor = Color(0.15f, 0.8f, 1f, 0.95f)
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
                    val releasePoint = event.modelVoxel?.cpy() ?: event.modelNextVoxel?.cpy()
                    val start = volumeSelectionStart
                    if (start != null && !draggingWindow && releasePoint != null) {
                        applyVolumeSelection(start, releasePoint)
                        volumeSelectionStart = null
                        feedback.clear()
                        pointerDown = false
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

                    if (pointerDownOnGround && releasePoint != null) {
                        volumeSelectionStart = releasePoint
                        updateHoverFeedback(event)
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
                    shapeRenderer.color = windowOutlineColor
                    shapeRenderer.rect(minX, minY, width, height)
                }

                override fun drawScreenOverlayFill(shapeRenderer: ShapeRenderer) {
                    if (!draggingWindow) {
                        return
                    }
                    val minX = minOf(dragStartStage.x, dragCurrentStage.x)
                    val minY = minOf(dragStartStage.y, dragCurrentStage.y)
                    val width = kotlin.math.abs(dragCurrentStage.x - dragStartStage.x)
                    val height = kotlin.math.abs(dragCurrentStage.y - dragStartStage.y)
                    shapeRenderer.color = windowFillColor
                    shapeRenderer.rect(minX, minY, width, height)
                }

                override fun reset() {
                    pointerDown = false
                    pointerDownOnGround = false
                    draggingWindow = false
                    volumeSelectionStart = null
                    feedback.clear()
                }

                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Clear Selection", {
                        selected.clear()
                        reset()
                    }),
                    ToolOperator("Reset", { reset() })
                )
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
                    if (acceptsToolClick(event)) {
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
            var mode = ApplyMode.ADD
            return object:EditorTool(
                name = name,
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if (acceptsToolClick(event)) {
                        when(state){
                            0 -> {
                                points[0]=event.modelNextVoxel!!.cpy()
                                state=1
                            }
                            1 -> {
                                points[1]=event.modelNextVoxel!!.cpy()
                                if (isRemoveMode(event, mode)) {
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
                            feedback.addCube(event.modelNextVoxel!!, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
                        }
                        1->{
                            rasterizer(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun reset() {
                    state=0
                    mode=ApplyMode.ADD
                    feedback.clear()
                }

                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Add", { mode = ApplyMode.ADD }, { mode == ApplyMode.ADD }),
                    ToolOperator("Remove", { mode = ApplyMode.REMOVE }, { mode == ApplyMode.REMOVE }),
                    ToolOperator("Reset", { reset() })
                )
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
                    if (acceptsToolClick(event)) {
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
            var mode = ApplyMode.ADD
            return object:EditorTool(
                name = "plane",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if (acceptsToolClick(event)) {
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
                                if (isRemoveMode(event, mode)) {
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
                            feedback.addCube(event.modelNextVoxel!!, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
                        }
                        1->{
                            voxelRangeSegment(points[0],event.modelNextVoxel!!){
                                feedback.addCube(it, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
                            }
                        }
                        2->{
                            voxelRangePlane(points[0],points[1],event.modelNextVoxel!!){
                                feedback.addCube(it, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
                            }
                        }
                    }
                    //currentEvent = event
                    return true
                }
            ){
                override fun reset() {
                    state=0
                    mode=ApplyMode.ADD
                    feedback.clear()
                }

                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Add", { mode = ApplyMode.ADD }, { mode == ApplyMode.ADD }),
                    ToolOperator("Remove", { mode = ApplyMode.REMOVE }, { mode == ApplyMode.REMOVE }),
                    ToolOperator("Reset", { reset() })
                )
            }
        }
        fun ArcEditor(scene: SceneController, feedback: SceneController):EditorTool{
            var voxelGen:( (p1: Vector3, p2: Vector3, p3: Vector3, callback: (p: Vector3) -> Unit) -> Unit)? = null
            val a = Color(1f,1f,0f,0.5f)
            val b = Color(1f,0.5f,0f,0.5f)
            val points= mutableListOf(Vector3(),Vector3(),Vector3())
            var state=0
            var mode = ApplyMode.ADD
            return object:EditorTool(
                name = "Arc",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if (acceptsToolClick(event)) {
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
                                if (isRemoveMode(event, mode)) {
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
                            feedback.addCube(event.modelNextVoxel!!, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
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
                                feedback.addCube(it, if (mode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
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
                    mode=ApplyMode.ADD
                    voxelGen=null
                    feedback.clear()
                }

                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Add", { mode = ApplyMode.ADD }, { mode == ApplyMode.ADD }),
                    ToolOperator("Remove", { mode = ApplyMode.REMOVE }, { mode == ApplyMode.REMOVE }),
                    ToolOperator("Reset", { reset() })
                )
            }
        }

        fun PolylineEditor(scene: SceneController, feedback: SceneController): EditorTool {
            val hoverColor = Color(1f, 1f, 0f, 0.5f)
            val anchorColor = Color(0.15f, 0.45f, 1f, 0.75f)
            val arcPointColor = Color(1f, 0.5f, 0f, 0.75f)
            var applyMode = ApplyMode.ADD
            var segmentMode = PolySegmentMode.LINE
            val objectPoints = mutableListOf<Vector3>()
            var anchor: Vector3? = null
            var arcPoint: Vector3? = null

            fun eventPoint(event: Vox3Event): Vector3? = event.modelNextVoxel ?: event.modelVoxel

            fun renderVoxel(point: Vector3) {
                if (applyMode == ApplyMode.REMOVE) {
                    scene.removeCube(point)
                } else {
                    scene.addCube(point)
                }
            }

            fun previewVoxel(point: Vector3) {
                feedback.addCube(point, if (applyMode == ApplyMode.REMOVE) Color.RED else scene.currentColor)
            }

            fun arcGenerator(start: Vector3): (p1: Vector3, p2: Vector3, p3: Vector3, callback: (p: Vector3) -> Unit) -> Unit {
                val dir = atan2(start.z.toDouble(), start.x.toDouble())
                return voxelRangeArcGenerator(if (dir < 0) -1 else 1)
            }

            fun appendLine(start: Vector3, end: Vector3) {
                val from = Vector3(start)
                val to = Vector3(end)
                voxelRangeSegment(from, to, ::renderVoxel)
                objectPoints.add(to)
                anchor = to
            }

            fun appendArc(start: Vector3, mid: Vector3, end: Vector3) {
                val from = Vector3(start)
                val through = Vector3(mid)
                val to = Vector3(end)
                arcGenerator(from)(from, through, to, ::renderVoxel)
                objectPoints.add(to)
                anchor = to
                arcPoint = null
            }

            fun appendCircle(center: Vector3, radiusPoint: Vector3) {
                voxelRangeCircle(Vector3(center), Vector3(radiusPoint), ::renderVoxel)
                objectPoints.add(Vector3(radiusPoint))
                anchor = Vector3(radiusPoint)
            }

            fun clearEditing() {
                objectPoints.clear()
                anchor = null
                arcPoint = null
                feedback.clear()
            }

            fun setSegmentMode(mode: PolySegmentMode) {
                segmentMode = mode
                arcPoint = null
                feedback.clear()
            }

            return object : EditorTool(
                name = "Polyline",
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    if (!acceptsToolClick(event)) {
                        return true
                    }
                    val point = eventPoint(event)?.cpy() ?: return true
                    val currentAnchor = anchor
                    if (currentAnchor == null) {
                        anchor = point
                        objectPoints.clear()
                        objectPoints.add(point)
                        feedback.clear()
                        return true
                    }

                    when (segmentMode) {
                        PolySegmentMode.LINE -> appendLine(currentAnchor, point)
                        PolySegmentMode.ARC -> {
                            val currentArcPoint = arcPoint
                            if (currentArcPoint == null) {
                                arcPoint = point
                            } else {
                                appendArc(currentAnchor, currentArcPoint, point)
                            }
                        }
                        PolySegmentMode.CIRCLE -> appendCircle(currentAnchor, point)
                    }
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
                    val point = eventPoint(event) ?: return true
                    feedback.clear()
                    val currentAnchor = anchor
                    if (currentAnchor == null) {
                        feedback.addCube(point, hoverColor)
                        return true
                    }

                    when (segmentMode) {
                        PolySegmentMode.LINE -> voxelRangeSegment(currentAnchor, point, ::previewVoxel)
                        PolySegmentMode.ARC -> {
                            val currentArcPoint = arcPoint
                            if (currentArcPoint == null) {
                                voxelRangeSegment(currentAnchor, point) { feedback.addCube(it, arcPointColor) }
                            } else {
                                arcGenerator(currentAnchor)(currentAnchor, currentArcPoint, point, ::previewVoxel)
                                feedback.addCube(currentArcPoint, arcPointColor)
                            }
                        }
                        PolySegmentMode.CIRCLE -> voxelRangeCircle(currentAnchor, point, ::previewVoxel)
                    }
                    feedback.addCube(objectPoints.first(), anchorColor)
                    feedback.addCube(currentAnchor, anchorColor)
                    return true
                }
            ) {
                override fun isObjectEditing(): Boolean = anchor != null

                override fun canFinishEditing(): Boolean = anchor != null

                override fun canCloseEditing(): Boolean = anchor != null && objectPoints.size >= 3

                override fun finishEditing(): Boolean {
                    if (anchor == null) {
                        return false
                    }
                    clearEditing()
                    return true
                }

                override fun closeEditing(): Boolean {
                    val start = objectPoints.firstOrNull() ?: return false
                    val currentAnchor = anchor ?: return false
                    if (objectPoints.size < 3) {
                        return false
                    }
                    if (currentAnchor.dst2(start) > 1e-8f) {
                        voxelRangeSegment(currentAnchor, start, ::renderVoxel)
                    }
                    clearEditing()
                    return true
                }

                override fun reset() {
                    applyMode = ApplyMode.ADD
                    setSegmentMode(PolySegmentMode.LINE)
                    clearEditing()
                }

                override fun toolOperators(): List<ToolOperator> = listOf(
                    ToolOperator("Add", { applyMode = ApplyMode.ADD }, { applyMode == ApplyMode.ADD }),
                    ToolOperator("Remove", { applyMode = ApplyMode.REMOVE }, { applyMode == ApplyMode.REMOVE }),
                    ToolOperator(PolySegmentMode.LINE.label, { setSegmentMode(PolySegmentMode.LINE) }, { segmentMode == PolySegmentMode.LINE }),
                    ToolOperator(PolySegmentMode.ARC.label, { setSegmentMode(PolySegmentMode.ARC) }, { segmentMode == PolySegmentMode.ARC }),
                    ToolOperator(PolySegmentMode.CIRCLE.label, { setSegmentMode(PolySegmentMode.CIRCLE) }, { segmentMode == PolySegmentMode.CIRCLE }),
                    ToolOperator("Reset", { reset() })
                )
            }
        }

        fun PointHelperEditor(
            name: String,
            feedback: SceneController,
            previewColor: Color,
            onPlace: (Vector3) -> Unit
        ): EditorTool {
            fun eventPoint(event: Vox3Event): Vector3? = event.modelVoxel ?: event.modelNextVoxel

            return object : EditorTool(
                name = name,
                onClick = fun(self: EditorTool, event: Vox3Event): Boolean {
                    val point = eventPoint(event) ?: return true
                    onPlace(Vector3(point))
                    feedback.clear()
                    return true
                },
                onMove = fun(self: EditorTool, event: Vox3Event): Boolean {
                    feedback.clear()
                    eventPoint(event)?.let { feedback.addCube(it, previewColor) }
                    return true
                }
            ) {
                override fun reset() {
                    feedback.clear()
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
    open fun drawScreenOverlayFill(shapeRenderer: ShapeRenderer) {}
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

    open fun isObjectEditing(): Boolean = false

    open fun canFinishEditing(): Boolean = isObjectEditing()

    open fun canCloseEditing(): Boolean = false

    open fun finishEditing(): Boolean = false

    open fun closeEditing(): Boolean = false

    open fun toolOperators(): List<ToolOperator> = listOf(
        ToolOperator("Reset", { reset() })
    )
}
