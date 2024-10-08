package com.xovd3i.editor

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.voxd31.editor.*
import com.voxd31.editor.exporters.readCubesCsv
import com.voxd31.editor.exporters.saveCubesAsCsv
import com.voxd31.gdxui.*
import java.io.File
import kotlin.math.floor


class Voxd31Editor(val filename:String="default.vxdi") : ApplicationAdapter() {
    companion object {

    }
    private val GNDSZ=100f
    private lateinit var camera3D: PerspectiveCamera
    private lateinit var camera2D: OrthographicCamera
    private lateinit var viewport3D: Viewport
    private lateinit var viewport2D: Viewport
    private lateinit var modelBatch: ModelBatch
    private lateinit var shadowBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var scene: SceneController
    private lateinit var selected: SceneController
    private lateinit var guides: SceneController
    private lateinit var feedback: SceneController
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var ground: ModelInstance
    private lateinit var sphere: Model
    private lateinit var inputProcessors: CompositeInputProcessor
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var shapeRenderer2d: ShapeRenderer
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var currentEvent: Vox3Event
    private lateinit var uiElements: UiElementsCollection


    val tools: MutableList<EditorTool> = mutableListOf() // Map activation keys to tools
    var activeTool: EditorTool? = null
    var activeToolIndex = 0
    lateinit var inputEventDispatcher: InputEventDispatcher
    val commands = mutableListOf<String>()
    var toolsCopy=false
    lateinit var uiTools: UiElementGrid


    private lateinit var cameraController: CameraInputController


    fun addTool(tool: EditorTool) {
        tools.add(tool)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun create() {
        // Fetch initial window dimensions
        val initialWidth = Gdx.graphics.width.toFloat()
        val initialHeight = Gdx.graphics.height.toFloat()
        spriteBatch = SpriteBatch()

        camera3D = PerspectiveCamera(45f, initialWidth, initialHeight).apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 300f
            fieldOfView=45f
            update()
        }
        shapeRenderer = ShapeRenderer()
        shapeRenderer2d = ShapeRenderer()


        modelBatch = ModelBatch()
        shadowBatch = ModelBatch(DepthShaderProvider())

        environment = Environment()
        shadowLight = DirectionalShadowLight(
            4096, 4096,
            60f, 60f, 1f,
            300f
        ).apply {
            set(0.5f, 0.5f, 0.5f, -0.5f, -1.8f, -1.2f)
            setColor(Color(0f,0f,0f,0.5f))
            environment.add(this)
            environment.shadowMap = this
            update(camera)
        }
        environment.add(DirectionalLight().set(0.5f, 0.5f, 0.5f, -0.5f, -1.8f, -1.2f).setColor(Color(0.5f,0.5f,0.5f,0.7f)))
        environment.add(DirectionalLight().set(0.1f, 0.1f, 0.1f, 1.2f, 1.8f, 0.5f).setColor(Color(0.1f,0.1f,0.1f,0.2f)))
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f,0.5f,0.5f, 0.7f)) // Reduced ambient light
        environment.set(ColorAttribute(ColorAttribute.Specular, 0.5f,0.5f,0.9f, 0.7f)) // Reduced ambient light


        viewport3D = ScreenViewport(camera3D)

        camera2D = OrthographicCamera()
        viewport2D = ExtendViewport(initialWidth, initialHeight, camera2D)
        viewport2D.apply(true)

        modelBuilder = ModelBuilder()
        scene = SceneController(modelBuilder)
        readCubesCsv(filename) { v:Vector3,c:Color ->
            scene.addCube(v,c)
        }
        guides = SceneController(modelBuilder)
        selected = SceneController(modelBuilder)
        feedback = SceneController(modelBuilder)
        feedback.currentColor = Color.GREEN

        val matGround = Material(ColorAttribute.createDiffuse(Color(0.3f,0.35f,0.3f,0.5f)))
        val groundBox = modelBuilder.createRect(
            -GNDSZ, 0f, -GNDSZ,
            -GNDSZ, 0f, GNDSZ,
            GNDSZ, 0f, GNDSZ,
            GNDSZ, 0f, -GNDSZ,
            0f, 1f, 0f,
            matGround, Usage.Position.toLong() or Usage.Normal.toLong())
        val matBullet = Material(ColorAttribute.createDiffuse(Color.LIME))
        sphere = modelBuilder.createSphere(0.5f,0.5f,0.5f,3,3,matBullet,Usage.Position.toLong() or Usage.Normal.toLong())

        ground = (ModelInstance(groundBox, 0f,-0.5f,0f))

        cameraController = EditorCameraController(camera3D)

        tools.add(EditorTool.SelectEditor(scene,feedback,selected))
        tools.add(EditorTool.makeTwoInputEditor("Select", onFeedback = { s:Vector3,e:Vector3 ->
            val cc=Color()
            cc.fromHsv(120f,0.8f,0.8f)
            cc.a=0.5f
            if(s!=e)selected.clear()
            feedback.clear()
            feedback.addCube(s,cc)
            voxelRangeShell(s,e){ p->
                feedback.addCube(p,cc)
            }
            voxelRangeVolume(s,e){
                p ->
                val c = scene.cubeAt(p)
                if(c!=null){
                    selected.cubes[c.getId()] = c
                }
            }
        }, onEnd = { s:Vector3,e:Vector3 ->
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf("//select ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z}")
        }))
        tools.add(EditorTool.makeTwoInputEditor("Move", onFeedback = { s:Vector3,e:Vector3 ->
            feedback.clear()
            val cc=Color()
            cc.fromHsv(120f,0.5f,1f)

            voxelRangeSegment(s,e){ p->
                feedback.addOrReplaceCube(p,Color.GOLD)
            }
            feedback.addOrReplaceCube(s,Color.RED)
            feedback.addOrReplaceCube(e,Color.GREEN)
            val delta = Vector3(floor(e.x)-floor(s.x), floor(e.y)-floor(s.y), floor(e.z)-floor(s.z))

            selected.cubes.forEach{ i,c->
                val sc = scene.cubes[i]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    feedback.addCube(sc.position.cpy().add(delta),cl)
                }
            }
        },onEnd={  s:Vector3,e:Vector3 ->
            val delta = Vector3(floor(e.x)-floor(s.x), floor(e.y)-floor(s.y), floor(e.z)-floor(s.z))
            val moved = selected.cubes.map{ kv->
                val sc = scene.cubes[kv.key]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    kv.value to Cube(sc.modelBuilder,sc.position.cpy().add(delta),cl)
                } else {
                    null
                }
            }.filterNotNull()
            selected.clear()
            synchronized(moved){
                moved.forEach{ (a,m) ->
                    scene.removeCube(a)
                }
            }
            synchronized(moved){
                moved.forEach{ (a,m) ->
                    scene.addCube(m.position,m.color)
                    selected.addCube(m.position,m.color)
                }
            }
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf("//move ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z}")
        }))
        tools.add(EditorTool.makeTwoInputEditor("Copy", onFeedback = { s:Vector3,e:Vector3 ->
            feedback.clear()
            val cc=Color()
            cc.fromHsv(120f,0.5f,1f)

            voxelRangeSegment(s,e){ p->
                feedback.addOrReplaceCube(p,Color.GOLD)
            }
            feedback.addOrReplaceCube(s,Color.RED)
            feedback.addOrReplaceCube(e,Color.GREEN)
            val delta = Vector3(floor(e.x)-floor(s.x), floor(e.y)-floor(s.y), floor(e.z)-floor(s.z))

            selected.cubes.forEach{ i,c->
                val sc = scene.cubes[i]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    feedback.addCube(sc.position.cpy().add(delta),cl)
                }
            }
        },onEnd={  s:Vector3,e:Vector3 ->
            val delta = Vector3(floor(e.x)-floor(s.x), floor(e.y)-floor(s.y), floor(e.z)-floor(s.z))
            val moved = selected.cubes.map{ kv->
                val sc = scene.cubes[kv.key]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    kv.value to Cube(sc.modelBuilder,sc.position.cpy().add(delta),cl)
                } else {
                    null
                }
            }.filterNotNull()
            selected.clear()
            synchronized(moved){
                moved.forEach{ (a,m) ->
                    scene.addCube(m.position,m.color)
                    selected.addCube(m.position,m.color)
                }
            }
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf("//copy ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z}")
        }))
        tools.add(EditorTool.makeThreeInputEditor("Rotate", onFeedback = { s:Vector3,m:Vector3,e:Vector3 ->
            feedback.clear()
            val cc=Color()
            cc.fromHsv(120f,0.5f,0.8f)

            voxelRangeSegment(s,m){ p->
                feedback.addCube(p,Color.GOLD)
            }
            voxelRangeSegment(s,e){ p->
                feedback.addCube(p,Color.ORANGE)
            }
            feedback.addCube(s,Color.RED)
            feedback.addCube(m,Color.GREEN)
            feedback.addCube(e,Color.BLUE)
            val rmx = calculateRotationMatrix(s,m,e)
            selected.cubes.forEach{ i,c->
                val sc = scene.cubes[i]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    feedback.addCube(sc.position.cpy().mul(rmx),cl)
                }
            }
        },onEnd={  s:Vector3,m:Vector3,e:Vector3 ->
            val rmx = calculateRotationMatrix(s,m,e)
            val moved = selected.cubes.map{ kv->
                val sc = scene.cubes[kv.key]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    kv.value to Cube(sc.modelBuilder,sc.position.cpy().mul(rmx),cl)
                } else {
                    null
                }
            }.filterNotNull()
            selected.clear()
            synchronized(moved){
                moved.forEach{ (a,m) ->
                    scene.removeCube(a)
                    scene.addCube(m.position,m.color)
                    selected.addCube(m.position,m.color)
                }
            }
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(m.x,m.y,m.z)
            val c=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf("//rotate ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${c.x} ${c.y} ${c.z}")
        }))
        tools.add(EditorTool.makeThreeInputEditor("CopyRot", onFeedback = { s:Vector3,m:Vector3,e:Vector3 ->
            feedback.clear()
            val cc=Color()
            cc.fromHsv(120f,0.5f,0.8f)

            voxelRangeSegment(s,m){ p->
                feedback.addCube(p,Color.GOLD)
            }
            voxelRangeSegment(s,e){ p->
                feedback.addCube(p,Color.ORANGE)
            }
            feedback.addCube(s,Color.RED)
            feedback.addCube(m,Color.GREEN)
            feedback.addCube(e,Color.BLUE)
            val rmx = calculateRotationMatrix(s,m,e)
            selected.cubes.forEach{ i,c->
                val sc = scene.cubes[i]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    feedback.addCube(sc.position.cpy().mul(rmx),cl)
                }
            }
        },onEnd={  s:Vector3,m:Vector3,e:Vector3 ->
            val rmx = calculateRotationMatrix(s,m,e)
            val moved = selected.cubes.map{ kv->
                val sc = scene.cubes[kv.key]
                if(sc!=null){
                    val cl=sc.color.cpy()
                    kv.value to Cube(sc.modelBuilder,sc.position.cpy().mul(rmx),cl)
                } else {
                    null
                }
            }.filterNotNull()
            selected.clear()
            synchronized(moved){
                moved.forEach{ (a,m) ->
                    scene.addCube(m.position,m.color)
                    selected.addCube(m.position,m.color)
                }
            }
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(m.x,m.y,m.z)
            val c=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf("//rotate ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${c.x} ${c.y} ${c.z}")
        }))
        tools.add(EditorTool.VoxelEditor(scene,feedback))
        tools.add(EditorTool.makeTwoInputEditor("Segment",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->

            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            val list=mutableListOf(
                "# Segment ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
            )
            voxelRangeSegment(s,e){ p->
                val a=Vector3i.fromFloats(p.x,p.y,p.z)
                op(p)
                list.add("/setblock ${a.x} ${a.y} ${a.z} minecraft:stone")
            }
            list
        })
        tools.add(EditorTool.ArcEditor(scene,feedback))
        tools.add(EditorTool.makeTwoInputEditor("Circle",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeCircle(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            val r = s.cpy().sub(e).len()
            listOf(
                "# Circle ${a.x} ${a.y} ${a.z} ${r} ${scene.currentColor}",
            )
        })
        tools.add(EditorTool.PlaneEditor(scene,feedback))
        tools.add(EditorTool.makeTwoInputEditor("Sphere",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeSphere(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Circle ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${a.x+1} ${a.y+1} ${a.z+1} ${b.x-1} ${b.y-1} ${b.z-1} air replace\n ",
            )
        })
        tools.add(EditorTool.makeTwoInputEditor("Cloud",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeCloudSphere(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Circle ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${a.x+1} ${a.y+1} ${a.z+1} ${b.x-1} ${b.y-1} ${b.z-1} air replace\n ",
            )
        })
        tools.add(EditorTool.makeTwoInputEditor("Ball",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeHollowSphere(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Circle ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${a.x+1} ${a.y+1} ${a.z+1} ${b.x-1} ${b.y-1} ${b.z-1} air replace\n ",
            )
        })
        tools.add(EditorTool.makeTwoInputEditor("Frame",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeFrame(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Frame ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${a.x} ${b.y} ${a.z} minecraft:stone",
                "/fill ${a.x} ${b.y} ${a.z} ${b.x} ${b.y} ${a.z} minecraft:stone",
                "/fill ${b.x} ${b.y} ${a.z} ${b.x} ${a.y} ${a.z} minecraft:stone",
                "/fill ${b.x} ${a.y} ${a.z} ${a.x} ${a.y} ${a.z} minecraft:stone",
                "/fill ${a.x} ${a.y} ${a.z} ${a.x} ${a.y} ${b.z} minecraft:stone",
                "/fill ${a.x} ${b.y} ${a.z} ${a.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${b.x} ${b.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${b.x} ${a.y} ${a.z} ${b.x} ${a.y} ${b.z} minecraft:stone",
                "/fill ${a.x} ${a.y} ${b.z} ${a.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${a.x} ${b.y} ${b.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${b.x} ${b.y} ${b.z} ${b.x} ${a.y} ${b.z} minecraft:stone",
                "/fill ${b.x} ${a.y} ${b.z} ${a.x} ${a.y} ${b.z} minecraft:stone",
            )
        })
        tools.add(EditorTool.makeTwoInputEditor("Shell",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeShell(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Shell ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
                "/fill ${a.x+1} ${a.y+1} ${a.z+1} ${b.x-1} ${b.y-1} ${b.z-1} air replace\n ",
            )
        })
        tools.add(EditorTool.makeTwoInputEditor("Volume",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeVolume(s,e,op)
            val a=Vector3i.fromFloats(s.x,s.y,s.z)
            val b=Vector3i.fromFloats(e.x,e.y,e.z)
            listOf(
                "# Volume ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} ${scene.currentColor}",
                "/fill ${a.x} ${a.y} ${a.z} ${b.x} ${b.y} ${b.z} minecraft:stone",
            )
        })

        println(tools.map{t -> t.name})

        activeTool = tools[activeToolIndex]

        inputEventDispatcher = InputEventDispatcher(scene,camera2D,camera3D,guides)
        inputProcessors= CompositeInputProcessor()
        inputProcessors.addInputProcessor(cameraController)
        inputProcessors.addInputProcessor(inputEventDispatcher)

        Gdx.input.inputProcessor = inputProcessors
        inputEventDispatcher.on("keyUp"){event ->
            when(event.keyCode){
                Input.Keys.DEL,
                Input.Keys.BACK,
                Input.Keys.FORWARD_DEL -> {
                    if(selected.cubes.size > 0) {
                        selected.cubes.forEach{
                            cube ->
                            scene.removeCube(cube.value)
                        }
                        selected.clear()
                    }
                }
                Input.Keys.T -> {
                    if(tools.size > 0) {
                        activeToolIndex=(activeToolIndex + 1) % tools.size
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")
                        activeTool = tools[activeToolIndex]
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")

                        activeTool!!.reset()
                    }
                }
                Input.Keys.R -> {
                    if(tools.size > 0) {
                        activeToolIndex=if(activeToolIndex < 1) tools.size -1 else activeToolIndex - 1
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")
                        activeTool = tools[activeToolIndex]
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")

                        activeTool!!.reset()
                    }
                }
                Input.Keys.G -> {
                    val mp = event.modelVoxel!!
                    guides.addCube(Vector3(mp),Color.WHITE)

                    for(i in 2 until 21) {
                        guides.addCube(Vector3(mp).set(mp.x+i.toFloat(),mp.y,mp.z),Color.RED)
                        guides.addCube(Vector3(mp).set(mp.x-i.toFloat(),mp.y,mp.z),Color.RED)
                        guides.addCube(Vector3(mp).set(mp.x,mp.y+i.toFloat(),mp.z),Color.BLUE)
                        guides.addCube(Vector3(mp).set(mp.x,mp.y-i.toFloat(),mp.z),Color.BLUE)
                        guides.addCube(Vector3(mp).set(mp.x,mp.y,mp.z+i.toFloat()),Color.GREEN)
                        guides.addCube(Vector3(mp).set(mp.x,mp.y,mp.z-i.toFloat()),Color.GREEN)
                    }
                }
                Input.Keys.S -> {
                    saveCubesAsCsv(scene.cubes.values.toList(),filename)
                }
                Input.Keys.SPACE -> {
                    saveCubesAsCsv(scene.cubes.values.toList(),filename)
                    if(guides.cubes.isNotEmpty()) {
                        guides.clear()
                    } else if(selected.cubes.isNotEmpty()) {
                        selected.clear()
                    } else if (activeToolIndex != 0) {
                        activeTool!!.reset()
                    }
                }
                Input.Keys.ESCAPE -> {
                    saveCubesAsCsv(scene.cubes.values.toList(),filename)
                    if(guides.cubes.isNotEmpty()) {
                        guides.clear()
                    } else if(selected.cubes.isNotEmpty()) {
                        selected.clear()
                    } else if (activeToolIndex != 0) {
                        activeTool!!.reset()
                        activeToolIndex = 0
                        activeTool = tools[activeToolIndex]
                        activeTool!!.reset()
                        uiTools.setSelected(0)
                    }
                }
                else -> {
                    println("key up : ${event.keyCode}")
                }
            }
            uiElements.dispatch(event)
            // currentEvent.keyCode = event.keyCode
            // currentEvent.keyDown = null
        }
        inputEventDispatcher.on("mouseMoved"){event ->

            uiElements.dispatch(event)
            if(!uiElements.isHovered) {
                activeTool?.onMove?.let { it(activeTool!!, event) }
                currentEvent = event
            }
            // currentEvent = event
        }
        inputEventDispatcher.on("touchUp"){event ->
            if(!uiElements.isClicked) {
                activeTool?.handleEvent(event)
                // activeTool?.onClick?.let { it(activeTool!!,event) }
                currentEvent = event
            }
            uiElements.dispatch(event)
            // currentEvent.button = event.button
        }
        inputEventDispatcher.on("touchDown"){event ->
            uiElements.dispatch(event)
            // currentEvent.button = event.button
        }
        inputEventDispatcher.on("keyDown"){event ->
            uiElements.dispatch(event)
            // currentEvent.keyCode = event.keyCode
            // currentEvent.keyDown = event.keyDown
        }
        inputEventDispatcher.on("keyUp"){event ->
            uiElements.dispatch(event)
            // currentEvent.keyCode = event.keyCode
            // currentEvent.keyDown = null
        }
        currentEvent= Vox3Event()


        // println(uiElements)
        //initUi(uiElements)

    }

    private var uiIsInitialized=0
    private fun initUi() {
        if (uiIsInitialized > 20){
            return
        }
        uiIsInitialized++
        uiElements = UiElementsCollection()
        val hueNumber=35
        val hueStep=10.0f
        val primaryColors = range(0,hueNumber).map { hue ->
            val bg = Color()
            bg.fromHsv(hue * hueStep, 1f, 0.7f)
            bg.a = 1f
            val color = Color()
            color.fromHsv(hue * hueStep, 1f, 1f)
            color.a = 1f
            val hexColor = if(hue<9) "111111ff" else "eeeeeeff"
            mapOf(
                "style" to UiStyleSheet(
                    text = (((bg.r*16).toInt()*256 ) + ((bg.g*16).toInt()*16) + ((bg.b*16).toInt())).toString(16).padStart(3, '0'),
                    normal = UiStyle(
                        background = bg,
                        color=Color.DARK_GRAY,
                        border=bg,
                        font = UIFont("NotoSans-Regular.ttf",12,Color.valueOf(hexColor))
                    ),
                    hover = UiStyle(
                        background = color,
                        color=Color.LIGHT_GRAY,
                        border=Color.CYAN,
                    ),
                    focus = UiStyle(
                        background = bg,
                        color=Color.LIGHT_GRAY,
                        border=Color.GOLD,
                        font = UIFont("NotoSans-Regular.ttf",12,Color.valueOf(hexColor))
                    )
                ),
                "index" to hue
            )
        }
        val transparentColors = range(0,hueNumber).map { hue ->
            val bg = Color()
            bg.fromHsv(hue * hueStep, 1f, 0.7f)
            bg.a = 0.5f
            val color = Color()
            color.fromHsv(hue * hueStep, 1f, 1f)
            color.a = 0.6f
            mapOf(
                "style" to UiStyleSheet(
                    normal = UiStyle(
                        background = bg,
                        color=Color.DARK_GRAY,
                        border=bg,
                        font = UIFont("NotoSans-Regular.ttf",12,Color.valueOf("111111ff"))
                    ),
                    hover = UiStyle(
                        background = color,
                        color=Color.LIGHT_GRAY,
                        border=Color.CYAN,
                    ),
                    focus = UiStyle(
                        background = bg,
                        color=Color.LIGHT_GRAY,
                        border=Color.GOLD,
                    ),
                    text = (((bg.r*16).toInt()*256 ) + ((bg.g*16).toInt()*16) + ((bg.b*16).toInt())).toString(16).padStart(3, '0'),
                ),
                "index" to hue
            )
        }
        val grayTones= range(0f,100f,5.05f).map{
                gs ->

            val hh = gs / 100f
            val hover = Color(0.5f, 0.5f, 0.8f, 1f)
            val tint = Color(hh, hh, hh, 1f)
            val dimmed = Color(hh, hh, hh, 1f)
            dimmed.a = 0.8f
            val font_id = if(gs < 30) "NotoSans-Regular 12px EEEEEEFF" else "NotoSans-Regular 12px 0A0A0AFF"
            mapOf(
                "style" to UiStyleSheet(
                    normal = UiStyle(
                        background = dimmed,
                        color=Color.DARK_GRAY,
                        border=dimmed,
                        font = UIFont.of(font_id),
                    ),
                    hover = UiStyle(
                        background = tint,
                        color=Color.LIGHT_GRAY,
                        border=Color.CYAN,
                        font = UIFont.of(font_id),
                    ),
                    focus = UiStyle(
                        background = dimmed,
                        color=Color.LIGHT_GRAY,
                        border=Color.GOLD,
                        font = UIFont.of(font_id),
                    ),
                    text = "${gs.toInt()}%",
                ),
                "index" to gs.toInt()
            )
        }
        val primaryColorsTable = primaryColors.groupBy { m ->
            val i = m["index"]!! as Int
            (i/6)
        }
        val transparentColorsTable = transparentColors.groupBy { m ->
            val i = m["index"]!! as Int
            (i/6)
        }
        val grayColorsTable = grayTones.groupBy { m ->
            val i = m["index"]!! as Int
            (i/25)
        }
        var x = 10f
        var y = 30f
        y = 30f
        y = 20f + 15f * 31f
        val toolsGridData = tools.mapIndexed{
            i,tool ->
            mapOf(
                "style" to UiStyleSheet(text=tool.name),
                "index" to i,
                "tool" to tool
            )
        }
        val toolsGridDataTable = toolsGridData.groupBy { m ->
            val i = m["index"]!! as Int
            (i/4)
        }
        y=viewport2D.worldHeight-32f
        uiElements.add(
            UiElementOptgroup<String>(
                position = Vector2(230f, y),
                size = Vector2(300f, 20f),
                label = "cube add modes : $y ${viewport2D.worldHeight} ${viewport2D.worldHeight-25}",
                options = listOf(
                    "addWithoutReplace",
                    "addOrReplace",
                    "replaceCube",
                )
            ) { target: UiElement, ev: Vox3Event, old:String, new:String ->
                println("changed add mode from $old to $new ")
                scene.addMode = new
            }.init()
        )
        val SZ1=Vector2(120f, 25f)
        val SZ2=Vector2(360f, 120f)
        uiElements.addAll( listOf(
            UiElementButton(
                position = Vector2(10f, y),
                size = Vector2(40f, 16f),
                radius = 8f,
                text = "ctrl",
            ) { target: UiElement, ev: Vox3Event ->
                target.hasFocus=ev.keypressedMap[Input.Keys.CONTROL_LEFT] != null
            },
            UiElementButton(
                position = Vector2(55f, y),
                size = Vector2(40f, 16f),
                radius = 8f,
                text = "shift",
            ) { target: UiElement, ev: Vox3Event ->
                target.hasFocus=ev.keypressedMap[Input.Keys.SHIFT_LEFT] != null
            },
            UiElementButton(
                position = Vector2(95f, y),
                size = Vector2(40f, 16f),
                radius = 8f,
                text = "alt",
            ) { target: UiElement, ev: Vox3Event ->
                target.hasFocus=ev.keypressedMap[Input.Keys.ALT_LEFT] != null
                //target.hover = if (kd) Color.DARK_GRAY else if (ku) Color.WHITE else target.color
            },
            UiElementButton(
                position = Vector2(140f, y),
                size = Vector2(60f, 16f),
                radius = 8f,
                text = "mouse",
            ) { target: UiElement, ev: Vox3Event ->
                val kd=  (ev.channel == "touchDown" && ev.button == Input.Buttons.LEFT)
                val ku= (ev.channel == "touchUp" && ev.button == Input.Buttons.LEFT)
                target.normalStyle.border = if (ev.keypressedMap[Input.Buttons.LEFT] != null) Color.GOLD else if (ku) Color.DARK_GRAY else target.normalStyle.background
                target.normalStyle.color = if (ev.keypressedMap[Input.Buttons.LEFT] != null) Color.LIGHT_GRAY else if (ku) Color.DARK_GRAY else Color.BLACK
            },))
        uiTools = UiElementGrid(
            elementSize= Vector2(85f,25f),
            data=toolsGridDataTable.values.map { it.map{ c -> c["style"]!! as UiStyleSheet} }
        ){ tp,ev,a,b ->
            println("grid element changed from $a to $b")
            println("switching tool ${tools[activeToolIndex].name} to ${tools[tp.selectedOrd].name}")
            activeToolIndex = tp.selectedOrd
            activeTool = tools[activeToolIndex]
            activeTool!!.reset()
        }
        uiElements.add(
            UiElementTabPanel(
                position = Vector2(10f,viewport2D.worldHeight-70f),
            ){ tp,ev,a,b ->
                println("tab panel changed from $a to $b")
            }.apply {
                tabs.addAll(
                    listOf(
                    UiElementButton(text="tools",size=SZ1.cpy()) to uiTools,
                    UiElementButton(text="primary",size=SZ1.cpy())
                        to UiElementGrid(
                            elementSize= Vector2(35f,25f),
                            data=primaryColorsTable.values.map { it.map{ c -> c["style"]!! as UiStyleSheet} }
                        ){ tp,ev,a,b ->
                            println("grid element changed from $a to $b")
                            scene.currentColor = b.hover.background
                        },
                    UiElementButton(text="transparent",size=SZ1.cpy())
                        to UiElementGrid(
                            elementSize= Vector2(35f,25f),
                            data=transparentColorsTable.values.map { it.map{ c -> c["style"]!! as UiStyleSheet} }
                        ){ tp,ev,a,b ->
                            println("grid element changed from $a to $b")
                            scene.currentColor = b.hover.background
                        },
                    UiElementButton(text="grayscale",size=SZ1.cpy())
                        to UiElementGrid(
                            elementSize= Vector2(35f,25f),
                            data=grayColorsTable.values.map { it.map{ c -> c["style"]!! as UiStyleSheet} }
                        ){ tp,ev,a,b ->
                            println("grid element changed from $a to $b")
                            scene.currentColor = b.hover.background
                        },
                    )
                )
            }.init(),
        )
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, viewport2D.screenX,viewport2D.screenY)
        Gdx.gl.glClearColor(0.5F, 0.9F, 0.9F, 1F); // Set a clear color different from your UI elements
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        initUi()


        // Process input and update the camera
        cameraController.update()
        camera3D.update()

        // render shadows
        shadowLight.begin(Vector3.Zero, camera3D.direction)
            shadowBatch.begin(shadowLight.camera)
                shadowBatch.render(scene.cubes.filter{c -> c.value.color.a > 0.99f}.map { (k, v) -> v.getModelInstance()})
                shadowBatch.render(feedback.cubes.filter{c -> c.value.color.a > 0.99f}.map { (k,v) -> v.getModelInstance() }, environment)
                shadowBatch.render(ground)
            shadowBatch.end()
        shadowLight.end()

        // render ground
        modelBatch.begin(camera3D)
        modelBatch.render(ground,environment)
        modelBatch.end()



        // render grid
        shapeRenderer.projectionMatrix = camera3D.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        renderGrid(
            shapeRenderer,
            camera3D,
            Color.LIGHT_GRAY,
            Color.GRAY,
            50,
            1,
            Vector3(-0.5f,-0.5f,-0.5f)
        )
        shapeRenderer.end()

        //render model
        modelBatch.begin(camera3D)
        modelBatch.render(scene.cubes.map { (k,v) -> v.getModelInstance() }, environment)
        modelBatch.render(feedback.cubes.map { (k,v) -> v.getModelInstance() }, environment)
        // modelBatch.render(ModelInstance(sphere, currentEvent.modelPoint),environment)
        // modelBatch.render(ModelInstance(sphere, currentEvent.modelNextPoint),environment)
        modelBatch.end()
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // render guides
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        guides.cubes.forEach { (k:String,cub:Cube) ->
            shapeRenderer.color = cub.color
            val bb=cub.getBoundingBox()
            // shapeRenderer.box(bb.min.x,bb.min.y,bb.max.z,bb.width,bb.height,bb.depth)
            val pad=0.40f
            shapeRenderer.box(bb.min.x+pad,bb.min.y+pad,bb.max.z-pad,bb.width-2*pad,bb.height-2*pad,bb.depth-2*pad)
            /// var c=Vector3()
            /// bb.getCenter(c)
            /// shapeRenderer.line(c.x-100,c.y,c.z,c.x+100,c.y,c.z,Color.RED,Color.RED)
            /// shapeRenderer.line(c.x,c.y-100,c.z,c.x,c.y+100,c.z,Color.GREEN,Color.GREEN)
            /// shapeRenderer.line(c.x,c.y,c.z-100,c.x,c.y,c.z+100,Color.BLUE,Color.BLUE)
        }
        selected.cubes.forEach { (k:String,cub:Cube) ->
            shapeRenderer.color = cub.color
            val bb=cub.getBoundingBox()
            shapeRenderer.box(bb.min.x,bb.min.y,bb.max.z,bb.width,bb.height,bb.depth)
            /// val pad=0.40f
            /// shapeRenderer.box(bb.min.x+pad,bb.min.y+pad,bb.max.z-pad,bb.width-2*pad,bb.height-2*pad,bb.depth-2*pad)
            /// /// var c=Vector3()
            /// /// bb.getCenter(c)
            /// /// shapeRenderer.line(c.x-100,c.y,c.z,c.x+100,c.y,c.z,Color.RED,Color.RED)
            /// /// shapeRenderer.line(c.x,c.y-100,c.z,c.x,c.y+100,c.z,Color.GREEN,Color.GREEN)
            /// /// shapeRenderer.line(c.x,c.y,c.z-100,c.x,c.y,c.z+100,Color.BLUE,Color.BLUE)
        }

        if(currentEvent.modelVoxel != null ) {
            shapeRenderer.color = Color.NAVY // Set the color of the grid lines
            shapeRenderer.line(currentEvent.modelNextPoint, currentEvent.modelNextPoint!!.cpy().add(currentEvent.normal))
            /////////// shapeRenderer.line(currentEvent.modelNextVoxel, currentEvent.modelNextVoxel!!.cpy().add(currentEvent.normal))
            shapeRenderer.color = Color.MAGENTA // Set the color of the grid lines
            shapeRenderer.line(currentEvent.modelPoint, currentEvent.modelPoint!!.cpy().add(currentEvent.normal))
            /////////////////// /////////// shapeRenderer.line(currentEvent.modelVoxel, currentEvent.modelVoxel!!.cpy().add(currentEvent.normal))
            /////////////////// shapeRenderer.color = Color.GREEN // Set the color of the grid lines
            /////////////////// shapeRenderer.line(currentEvent.modelPoint, currentEvent.modelPoint!!.cpy().sub(-0.5f,-0.5f, currentEvent.modelPoint!!.z))
            ///////////////////
            /////////////////// shapeRenderer.color = Color.BLUE // Set the color of the grid lines
            /////////////////// shapeRenderer.line(currentEvent.modelPoint, currentEvent.modelPoint!!.cpy().sub(-0.5f, currentEvent.modelPoint!!.y,-0.5f))
            ///////////////////
            /////////////////// shapeRenderer.color = Color.RED // Set the color of the grid lines
            /////////////////// shapeRenderer.line(currentEvent.modelPoint, currentEvent.modelPoint!!.cpy().sub( currentEvent.modelPoint!!.x,-0.5f,-0.5f))
        }
        shapeRenderer.end()


        shapeRenderer2d.projectionMatrix = camera2D.combined
        shapeRenderer2d.begin(ShapeRenderer.ShapeType.Line)
        uiElements.drawLines(shapeRenderer2d)
        shapeRenderer2d.end()
        shapeRenderer2d.begin(ShapeRenderer.ShapeType.Filled)
        uiElements.draw(shapeRenderer2d)
        if(uiElements.isHovered && currentEvent.screen?.x != null && currentEvent.screen?.x != null) {
            val sc = currentEvent.screen!!
            val cl = shapeRenderer2d.color
            val clRED=Color(1f,0f,0f,0.5f)
            val clGREEN=Color(0f,1f,0f,0.5f)
            shapeRenderer2d.color = if(uiElements.isPressed) clRED else clGREEN
            shapeRenderer2d.circle(sc.x, sc.y, 15f)
            shapeRenderer2d.color = cl
        }
        val dg=Color.DARK_GRAY
        shapeRenderer2d.rect(0f,0f,viewport2D.worldWidth,25f,dg,dg,dg,dg)
        shapeRenderer2d.end()

        spriteBatch.projectionMatrix = camera2D.combined
        spriteBatch.begin()

        uiElements.drawText(spriteBatch)
        if(currentEvent.screen != null) {
            UIFont.default().bitmapFont().draw(
                spriteBatch,
                """
                    ui:${viewport2D.worldWidth}x${viewport2D.worldHeight} cubes:${scene.cubes.size} xy:${currentEvent.screen} raw:${currentEvent.modelPoint},next:${currentEvent.modelNextPoint} int:${currentEvent.modelVoxel},next:${currentEvent.modelNextVoxel} n: ${currentEvent.normal}
                """.trimIndent(),
                10f,20f,
            ) // Draws text at the specified position.
            UIFont.default().bitmapFont().draw(
                spriteBatch,
                """${activeTool!!.name} ${currentEvent.modelVoxel}""".trimIndent(),
                currentEvent.screen!!.x, currentEvent.screen!!.y+15f,
            ) // Draws text at the specified position.
        }
        spriteBatch.end()

        shapeRenderer2d.end()
    }

    private fun renderGrid(
        shapeRenderer: ShapeRenderer,
        camera: Camera,
        gridColorMain: Color,
        gridColorSecondary:Color,
        gridSize: Int,
        lineSpacing:Int,
        anchor: Vector3,
    ) {

        // Draw grid lines parallel to the X axis
        for (z in -gridSize until (gridSize + lineSpacing) step lineSpacing) {
            if (z == -gridSize || z == gridSize || z == 0 || z % 5 == 0) {
                if (z == 0) {
                    shapeRenderer.color = Color.RED // Set the color of the grid lines
                } else {
                    shapeRenderer.color = gridColorMain // Set the color of the grid lines
                }
            } else {
                shapeRenderer.color = gridColorSecondary // Set the color of the grid lines
            }
            shapeRenderer.line(
                anchor.x-gridSize.toFloat(), anchor.y, anchor.z + z.toFloat(),
                anchor.z+gridSize.toFloat(), anchor.y, anchor.z+z.toFloat(),
            )
        }

        // Draw grid lines parallel to the Z axis
        for (x in -gridSize until (gridSize + lineSpacing) step lineSpacing) {
            if (x == -gridSize || x == gridSize || x == 0 || x % 5 == 0) {
                if (x == 0) {
                    shapeRenderer.color = Color.GREEN // Set the color of the grid lines
                } else {
                    shapeRenderer.color = gridColorMain // Set the color of the grid lines
                }
            } else {
                shapeRenderer.color = gridColorSecondary // Set the color of the grid lines
            }
            shapeRenderer.line(
                anchor.x+x.toFloat(), anchor.y, anchor.z-gridSize.toFloat(),
                anchor.x+x.toFloat(), anchor.y, anchor.z+gridSize.toFloat(),
            )
        }
        shapeRenderer.color = Color.BLUE // Set the color of the grid lines
        shapeRenderer.line(anchor.x,  -gridSize.toFloat(),anchor.z, anchor.x, gridSize.toFloat(), anchor.z)
    }

    override fun dispose() {
        modelBatch.dispose()
        shadowBatch.dispose()
        scene.dispose()
        guides.dispose()
        feedback.dispose()
        shadowLight.dispose()

        // If you set a different input processor later, you might need to do this
        if (Gdx.input.inputProcessor == cameraController) {
            Gdx.input.inputProcessor = null
        }
        saveCubesAsCsv(scene.cubes.values.toList(),filename)
        // saveSchematicToFile(scene.cubes.values.toList(), "$filename.schematic")
        val text = tools.flatMap { tool -> tool.commands }.joinToString("\n")
        File("$filename.mccmd").appendText(text)
    }

    override fun resize(width: Int, height: Int) {
        viewport3D.update(width, height, false);
        viewport2D.update(width, height, true);

        camera2D.position.set(camera2D.viewportWidth / 2, camera2D.viewportHeight / 2, 0f);
        camera2D.update();
        // Update the camera with the new window size
        camera3D.viewportWidth = width.toFloat()
        camera3D.viewportHeight = height.toFloat()
        camera3D.update()
    }

}