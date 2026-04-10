package com.xovd3i.editor

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.voxd31.editor.*
import com.voxd31.editor.exporters.appendTextFile
import com.voxd31.editor.exporters.exportSceneMesh
import com.voxd31.editor.exporters.loadModelFromCsv
import com.voxd31.editor.exporters.meshExportOptionForExtension
import com.voxd31.editor.exporters.meshExportOptions
import com.voxd31.editor.exporters.resolveReadableHandle
import com.voxd31.editor.exporters.resolveWritableHandle
import com.voxd31.editor.exporters.saveModelAsCsv
import com.voxd31.editor.ui.VoxcraftUiOverlay
import com.voxd31.gdxui.Cube
import com.voxd31.gdxui.Vox3Event
import com.kotcrab.vis.ui.VisUI
import kotlin.math.floor


class Voxd31Editor @JvmOverloads constructor(
    initialFilename: String = "default.vxdi",
    private val fileDialogService: FileDialogService = NoopFileDialogService
) : ApplicationAdapter() {
    companion object {

    }
    private val groundPlaneY = -0.5f
    private val gridPlaneY = groundPlaneY + 0.03f
    private val minimumGroundPlaneWidth = 12f
    private val minimumGroundPlaneDepth = 12f
    private val groundPlanePadding = 4f
    private val cameraTarget = Vector3()
    private val shadowBounds = BoundingBox()
    private val shadowBoundsCenter = Vector3()
    private val shadowBoundsDimensions = Vector3()
    private val shadowLightDirection = Vector3(-0.5f, -1.8f, -1.2f).nor()
    private val minimumShadowBoundsRadius = 17.320509f
    private var shadowModelBoundsRadius = minimumShadowBoundsRadius
    private var shadowModelBoundsValid = false
    private var orthoDistance = 18f
    private var activeOrthoView = OrthographicView.TOP
    private lateinit var orbitCamera: PerspectiveCamera
    private lateinit var walkCamera: PerspectiveCamera
    private lateinit var orthoCamera: OrthographicCamera
    private lateinit var activeCamera: Camera
    private lateinit var activeCameraInputProcessor: InputProcessor
    private lateinit var orbitCameraController: ShiftCameraController
    private lateinit var walkthroughCameraController: WalkthroughCameraController
    private lateinit var orthoCameraController: OrthographicCameraController
    private var activeCameraMode = CameraMode.ORBIT
    private lateinit var modelBatch: ModelBatch
    private lateinit var shadowBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var shadowLight: ResizableDirectionalShadowLight
    private lateinit var scene: SceneController
    private lateinit var selected: SceneController
    private lateinit var guides: SceneController
    private lateinit var feedback: SceneController
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var groundModel: Model
    private lateinit var ground: ModelInstance
    private lateinit var sphere: Model
    private lateinit var inputProcessors: InputMultiplexer
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var currentEvent: Vox3Event
    private lateinit var uiOverlay: VoxcraftUiOverlay
    private val versionInfo = Voxd31EditorVersion()
    private var filename: String = initialFilename
    private var modelSettings = ModelSettings()
    private var statusMessage = ""
    private val groundCenter = Vector3()
    private var groundWidth = minimumGroundPlaneWidth
    private var groundDepth = minimumGroundPlaneDepth


    val tools: MutableList<EditorTool> = mutableListOf() // Map activation keys to tools
    var activeTool: EditorTool? = null
    var activeToolIndex = 0
    lateinit var inputEventDispatcher: InputEventDispatcher
    val commands = mutableListOf<String>()


    fun addTool(tool: EditorTool) {
        tools.add(tool)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun create() {
        loadVisUi()
        val initialWidth = Gdx.graphics.width.toFloat()
        val initialHeight = Gdx.graphics.height.toFloat()

        orbitCamera = PerspectiveCamera(45f, initialWidth, initialHeight).apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 0.1f
            far = 300f
            fieldOfView=45f
            update()
        }
        walkCamera = PerspectiveCamera(45f, initialWidth, initialHeight).apply {
            position.set(orbitCamera.position)
            direction.set(orbitCamera.direction)
            up.set(orbitCamera.up)
            near = 0.1f
            far = 300f
            fieldOfView = 45f
            update()
        }
        orthoCamera = OrthographicCamera().apply {
            near = -2000f
            far = 2000f
            zoom = 1f
        }
        configureOrthoViewport(Gdx.graphics.width, Gdx.graphics.height)
        alignOrthographicView(activeOrthoView)

        shapeRenderer = ShapeRenderer()

        modelBatch = ModelBatch()
        shadowBatch = ModelBatch(DepthShaderProvider())

        environment = Environment()
        shadowLight = ResizableDirectionalShadowLight(
            8192, 8192,
            96f, 96f, 1f,
            360f
        ).apply {
            set(0.5f, 0.5f, 0.5f, shadowLightDirection.x, shadowLightDirection.y, shadowLightDirection.z)
            setColor(Color(0f,0f,0f,0.5f))
            environment.add(this)
            environment.shadowMap = this
            update(camera)
        }
        environment.add(
            DirectionalLight()
                .set(0.5f, 0.5f, 0.5f, shadowLightDirection.x, shadowLightDirection.y, shadowLightDirection.z)
                .setColor(Color(0.5f,0.5f,0.5f,0.7f))
        )
        environment.add(DirectionalLight().set(0.1f, 0.1f, 0.1f, 1.2f, 1.8f, 0.5f).setColor(Color(0.1f,0.1f,0.1f,0.2f)))
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f,0.5f,0.5f, 0.7f)) // Reduced ambient light
        environment.set(ColorAttribute(ColorAttribute.Specular, 0.5f,0.5f,0.9f, 0.7f)) // Reduced ambient light

        modelBuilder = ModelBuilder()
        scene = SceneController(modelBuilder)
        guides = SceneController(modelBuilder)
        selected = SceneController(modelBuilder)
        feedback = SceneController(modelBuilder)
        feedback.currentColor = Color.GREEN

        val matBullet = Material(ColorAttribute.createDiffuse(Color.LIME))
        sphere = modelBuilder.createSphere(0.5f,0.5f,0.5f,3,3,matBullet,Usage.Position.toLong() or Usage.Normal.toLong())

        loadModelFromDisk(filename, announce = false)
        rebuildGroundPlane(force = true)
        updateWindowTitle()
        orbitCameraController = ShiftCameraController(orbitCamera, this::pickOrbitModelPoint).apply {
            rotateButton = Input.Buttons.RIGHT
            translateButton = Input.Buttons.RIGHT
            target.set(cameraTarget)
        }
        walkthroughCameraController = WalkthroughCameraController(
            walkCamera,
            this::walkSupportHeightAt,
            eyeHeight = 3f
        )
        orthoCameraController = OrthographicCameraController(orthoCamera, cameraTarget)
        setCameraMode(CameraMode.ORBIT)

        tools.add(EditorTool.SelectEditor(scene, feedback, selected, this::queryCubesInScreenRect))
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
                    selected.addCube(c.position, c.color)
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
        currentEvent = Vox3Event()

        uiOverlay = VoxcraftUiOverlay(
            toolNamesProvider = { tools.map { it.name } },
            activeToolIndexProvider = { activeToolIndex },
            toolSelected = { index ->
                activeToolIndex = index
                activeTool = tools[activeToolIndex]
                activeTool?.reset()
            },
            currentColorProvider = { scene.currentColor },
            colorSelected = { color -> scene.currentColor = color },
            addModeProvider = { scene.addMode },
            addModeChanged = { mode -> scene.addMode = mode },
            cameraModeProvider = { activeCameraMode },
            cameraModeChanged = { mode -> setCameraMode(mode) },
            orthographicViewChanged = { view -> setOrthographicView(view) },
            openAction = { openModelDialog() },
            saveAction = { saveCurrentModel() },
            saveAsAction = { saveModelAsDialog() },
            exportMeshAction = { exportMeshDialog() },
            modelSettingsProvider = { modelSettings.copy() },
            modelSettingsChanged = { settings -> applyModelSettings(settings) },
            clearSelectionAction = { selected.clear() },
            clearGuidesAction = { guides.clear() },
            resetToolAction = { activeTool?.reset() },
            statusProvider = { uiStatusSnapshot() }
        )

        inputEventDispatcher = InputEventDispatcher(
            scene = scene,
            activeCameraProvider = { activeCamera },
            guides = guides,
            screenToUi = { x, y -> uiOverlay.stage.screenToStageCoordinates(Vector2(x.toFloat(), y.toFloat())) }
        )
        val activeCameraProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean = activeCameraInputProcessor.keyDown(keycode)
            override fun keyUp(keycode: Int): Boolean = activeCameraInputProcessor.keyUp(keycode)
            override fun keyTyped(character: Char): Boolean = activeCameraInputProcessor.keyTyped(character)
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
                activeCameraInputProcessor.touchDown(screenX, screenY, pointer, button)

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
                activeCameraInputProcessor.touchUp(screenX, screenY, pointer, button)

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean =
                activeCameraInputProcessor.touchDragged(screenX, screenY, pointer)

            override fun mouseMoved(screenX: Int, screenY: Int): Boolean =
                activeCameraInputProcessor.mouseMoved(screenX, screenY)

            override fun scrolled(amountX: Float, amountY: Float): Boolean =
                activeCameraInputProcessor.scrolled(amountX, amountY)
        }
        inputProcessors = InputMultiplexer(uiOverlay.stage, activeCameraProcessor, inputEventDispatcher)

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
                    saveCurrentModel()
                }
                Input.Keys.NUM_1 -> {
                    setCameraMode(CameraMode.ORBIT)
                }
                Input.Keys.NUM_2 -> {
                    setCameraMode(CameraMode.WALKTHROUGH)
                }
                Input.Keys.NUM_3 -> {
                    setOrthographicView(OrthographicView.TOP)
                }
                Input.Keys.NUM_4 -> {
                    setOrthographicView(OrthographicView.FRONT)
                }
                Input.Keys.SPACE -> {
                    saveCurrentModel()
                    if(guides.cubes.isNotEmpty()) {
                        guides.clear()
                    } else if(selected.cubes.isNotEmpty()) {
                        selected.clear()
                    } else if (activeToolIndex != 0) {
                        activeTool!!.reset()
                    }
                }
                Input.Keys.ESCAPE -> {
                    saveCurrentModel()
                    if(guides.cubes.isNotEmpty()) {
                        guides.clear()
                    } else if(selected.cubes.isNotEmpty()) {
                        selected.clear()
                    } else if (activeToolIndex != 0) {
                        activeTool!!.reset()
                        activeToolIndex = 0
                        activeTool = tools[activeToolIndex]
                        activeTool!!.reset()
                    }
                }
                else -> {
                    println("key up : ${event.keyCode}")
                }
            }
            currentEvent = event
        }
        inputEventDispatcher.on("mouseMoved"){event ->
            activeTool?.onMove?.let { it(activeTool!!, event) }
            currentEvent = event
        }
        inputEventDispatcher.on("touchDragged"){event ->
            if (event.pointer == 0) {
                activeTool?.touchDragged(event)
            }
            currentEvent = event
        }
        inputEventDispatcher.on("touchUp"){event ->
            if (event.button == Input.Buttons.LEFT) {
                activeTool?.touchUp(event)
            }
            currentEvent = event
        }
        inputEventDispatcher.on("touchDown"){event ->
            if (event.button == Input.Buttons.LEFT) {
                activeTool?.touchDown(event)
            }
            currentEvent = event
        }
        inputEventDispatcher.on("keyDown"){event ->
            currentEvent = event
        }
        inputEventDispatcher.on("keyUp"){event ->
            currentEvent = event
        }
    }

    private fun loadVisUi() {
        if (VisUI.isLoaded()) {
            return
        }
        if (Gdx.app.type == Application.ApplicationType.WebGL) {
            VisUI.setSkipGdxVersionCheck(true)
            requireWebInternalFile("com/kotcrab/vis/ui/skin/x1/uiskin.json")
            requireWebInternalFile("com/kotcrab/vis/ui/skin/x1/uiskin.atlas")
            requireWebInternalFile("com/kotcrab/vis/ui/skin/x1/uiskin.png")
            requireWebInternalFile("com/kotcrab/vis/ui/skin/x1/default.fnt")
            requireWebInternalFile("com/kotcrab/vis/ui/skin/x1/font-small.fnt")
            VisUI.load(Gdx.files.internal("com/kotcrab/vis/ui/skin/x1/uiskin.json"))
            return
        }
        VisUI.load()
    }

    private fun requireWebInternalFile(path: String): FileHandle {
        val file = Gdx.files.internal(path)
        if (!file.exists()) {
            throw IllegalStateException("Required web asset not found (internal): $path")
        }
        return file
    }

    private fun configureOrthoViewport(width: Int, height: Int) {
        val safeHeight = height.coerceAtLeast(1)
        val aspect = width.coerceAtLeast(1).toFloat() / safeHeight.toFloat()
        val worldHeight = 22f
        orthoCamera.viewportHeight = worldHeight
        orthoCamera.viewportWidth = worldHeight * aspect
        orthoCamera.update()
    }

    private fun setCameraMode(mode: CameraMode) {
        if (!::orbitCamera.isInitialized || !::walkCamera.isInitialized || !::orthoCamera.isInitialized) {
            return
        }
        if (::activeCamera.isInitialized && activeCameraMode == mode) {
            return
        }
        when (mode) {
            CameraMode.ORBIT -> {
                copyPoseToPerspective(activeCameraOrNull(), orbitCamera, keepTarget = true)
                orbitCameraController.target.set(cameraTarget)
                activeCamera = orbitCamera
                activeCameraInputProcessor = orbitCameraController
            }

            CameraMode.WALKTHROUGH -> {
                copyPoseToPerspective(activeCameraOrNull(), walkCamera, keepTarget = false)
                walkthroughCameraController.syncFromCamera()
                activeCamera = walkCamera
                activeCameraInputProcessor = walkthroughCameraController
            }

            CameraMode.ORTHOGRAPHIC -> {
                updateTargetFromPerspective(activeCameraOrNull())
                activeCamera = orthoCamera
                activeCameraInputProcessor = orthoCameraController
                alignOrthographicView(activeOrthoView)
            }
        }
        activeCameraMode = mode
    }

    private fun setOrthographicView(view: OrthographicView) {
        activeOrthoView = view
        setCameraMode(CameraMode.ORTHOGRAPHIC)
        alignOrthographicView(view)
    }

    private fun alignOrthographicView(view: OrthographicView) {
        val direction = Vector3()
        val up = Vector3()
        when (view) {
            OrthographicView.TOP -> {
                direction.set(0f, -1f, 0f)
                up.set(0f, 0f, -1f)
            }

            OrthographicView.BOTTOM -> {
                direction.set(0f, 1f, 0f)
                up.set(0f, 0f, 1f)
            }

            OrthographicView.LEFT -> {
                direction.set(1f, 0f, 0f)
                up.set(0f, 1f, 0f)
            }

            OrthographicView.RIGHT -> {
                direction.set(-1f, 0f, 0f)
                up.set(0f, 1f, 0f)
            }

            OrthographicView.FRONT -> {
                direction.set(0f, 0f, -1f)
                up.set(0f, 1f, 0f)
            }

            OrthographicView.BACK -> {
                direction.set(0f, 0f, 1f)
                up.set(0f, 1f, 0f)
            }
        }
        orthoCamera.direction.set(direction).nor()
        orthoCamera.up.set(up).nor()
        orthoCamera.position.set(cameraTarget).sub(direction.scl(orthoDistance))
        orthoCamera.update()
    }

    private fun updateActiveCamera(deltaTime: Float) {
        when (activeCameraMode) {
            CameraMode.ORBIT -> {
                orbitCameraController.update()
                cameraTarget.set(orbitCameraController.target)
                orbitCamera.up.set(0f, 1f, 0f)
                orbitCamera.lookAt(cameraTarget)
                orbitCamera.update()
            }

            CameraMode.WALKTHROUGH -> {
                walkthroughCameraController.update(deltaTime)
                cameraTarget.set(walkCamera.position).mulAdd(walkCamera.direction, 8f)
                walkCamera.update()
            }

            CameraMode.ORTHOGRAPHIC -> {
                orthoCameraController.update()
                orthoCamera.update()
            }
        }
    }

    private fun activeCameraOrNull(): Camera? {
        return if (::activeCamera.isInitialized) activeCamera else null
    }

    private fun updateTargetFromPerspective(source: Camera?) {
        val perspective = source as? PerspectiveCamera ?: return
        if (perspective === orbitCamera) {
            return
        }
        cameraTarget.set(perspective.position).mulAdd(perspective.direction, 8f)
    }

    private fun copyPoseToPerspective(source: Camera?, target: PerspectiveCamera, keepTarget: Boolean) {
        when (source) {
            is PerspectiveCamera -> {
                target.position.set(source.position)
                target.direction.set(source.direction).nor()
                target.up.set(source.up).nor()
                if (keepTarget) {
                    cameraTarget.set(source.position).mulAdd(source.direction, 8f)
                }
            }

            is OrthographicCamera -> {
                val dir = Vector3(source.direction).nor()
                if (dir.len2() <= 1e-8f) {
                    dir.set(0f, -1f, 0f)
                }
                target.position.set(cameraTarget).sub(dir.scl(12f))
                target.up.set(source.up).nor()
                target.lookAt(cameraTarget)
            }

            else -> {
                target.lookAt(cameraTarget)
            }
        }
        target.update()
    }

    private fun setStatusMessage(message: String) {
        statusMessage = message
        println(message)
    }

    private fun displayFileName(path: String): String {
        val normalized = path.replace('\\', '/')
        return normalized.substringAfterLast('/')
    }

    private fun directoryHint(path: String): String? {
        val normalized = path.replace('\\', '/')
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash > 0) normalized.substring(0, lastSlash) else null
    }

    private fun baseName(path: String): String {
        val name = displayFileName(path)
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    private fun extensionOf(path: String): String {
        val name = displayFileName(path)
        val dot = name.lastIndexOf('.')
        return if (dot >= 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else ""
    }

    private fun ensureExtension(path: String, extension: String): String {
        if (extensionOf(path).isNotEmpty()) {
            return path
        }
        return "$path.$extension"
    }

    private fun updateWindowTitle() {
        Gdx.graphics.setTitle(
            "voxcraft   version : ${versionInfo.buildVersion}   file : [${displayFileName(filename)}]"
        )
    }

    private fun applyModelSettings(settings: ModelSettings) {
        modelSettings = settings.copy(
            gridSize = settings.gridSize.coerceAtLeast(1),
            unitSize = settings.unitSize.coerceAtLeast(1e-6f),
            unitSuffix = settings.unitSuffix.ifBlank { "unit" }
        )
        setStatusMessage(
            "Model settings updated: grid=${modelSettings.gridSize}, unit=${modelSettings.unitSize} ${modelSettings.unitSuffix}"
        )
    }

    private fun loadModelFromDisk(path: String, announce: Boolean = true) {
        val source = resolveReadableHandle(path)
        val exists = source.exists()
        val loaded = loadModelFromCsv(path)
        scene.clear()
        selected.clear()
        guides.clear()
        feedback.clear()
        loaded.cubes.forEach { (position, color) ->
            scene.addCube(position, color)
        }
        modelSettings = loaded.settings.copy(
            gridSize = loaded.settings.gridSize.coerceAtLeast(1),
            unitSize = loaded.settings.unitSize.coerceAtLeast(1e-6f),
            unitSuffix = loaded.settings.unitSuffix.ifBlank { "unit" }
        )
        filename = path
        activeTool?.reset()
        rebuildGroundPlane(force = true)
        updateWindowTitle()
        if (announce) {
            if (exists) {
                setStatusMessage("Loaded ${displayFileName(filename)}")
            } else {
                setStatusMessage("Started new model ${displayFileName(filename)}")
            }
        }
    }

    private fun saveCurrentModel(announce: Boolean = true) {
        saveModelAsCsv(scene.cubes.values.toList(), filename, modelSettings)
        rebuildGroundPlane(force = true)
        updateWindowTitle()
        if (announce) {
            setStatusMessage("Saved ${displayFileName(filename)}")
        }
    }

    private fun openModelDialog() {
        if (!fileDialogService.isSupported()) {
            setStatusMessage("Open dialog is unavailable in this runtime.")
            return
        }
        val path = fileDialogService.openFile(
            title = "Open Voxcraft Model",
            directoryHint = directoryHint(filename),
            defaultFileName = displayFileName(filename),
            allowedExtensions = setOf("vxdi")
        ) ?: run {
            setStatusMessage("Open canceled.")
            return
        }
        loadModelFromDisk(path)
    }

    private fun saveModelAsDialog() {
        if (!fileDialogService.isSupported()) {
            setStatusMessage("Save As dialog is unavailable in this runtime.")
            return
        }
        val requested = fileDialogService.saveFile(
            title = "Save Voxcraft Model As",
            directoryHint = directoryHint(filename),
            defaultFileName = displayFileName(filename),
            allowedExtensions = setOf("vxdi")
        ) ?: run {
            setStatusMessage("Save As canceled.")
            return
        }
        filename = ensureExtension(requested, "vxdi")
        saveCurrentModel()
    }

    private fun exportMeshDialog() {
        if (scene.cubes.isEmpty()) {
            setStatusMessage("Export failed: the model is empty.")
            return
        }
        if (!fileDialogService.isSupported()) {
            setStatusMessage("Export dialog is unavailable in this runtime.")
            return
        }
        val requested = fileDialogService.saveFile(
            title = "Export Mesh",
            directoryHint = directoryHint(filename),
            defaultFileName = "${baseName(filename)}.obj",
            allowedExtensions = meshExportOptions.flatMap { it.extensions }.toSet()
        ) ?: run {
            setStatusMessage("Export canceled.")
            return
        }
        var targetPath = requested
        var extension = extensionOf(targetPath)
        if (extension.isEmpty()) {
            targetPath = ensureExtension(targetPath, "obj")
            extension = "obj"
        }
        val option = meshExportOptionForExtension(extension) ?: run {
            setStatusMessage("Export failed: unsupported extension .$extension")
            return
        }
        try {
            val bytes = exportSceneMesh(scene, option.format, modelSettings)
            resolveWritableHandle(targetPath).writeBytes(bytes, false)
            setStatusMessage("Exported ${displayFileName(targetPath)}")
        } catch (t: Throwable) {
            setStatusMessage("Export failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun rebuildGroundPlane(force: Boolean = false) {
        var minX = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        scene.cubes.values.forEach { cube ->
            val bounds = cube.getBoundingBox()
            minX = minOf(minX, bounds.min.x)
            minZ = minOf(minZ, bounds.min.z)
            maxX = maxOf(maxX, bounds.max.x)
            maxZ = maxOf(maxZ, bounds.max.z)
        }

        val newWidth: Float
        val newDepth: Float
        val newCenterX: Float
        val newCenterZ: Float
        if (scene.cubes.isEmpty()) {
            newWidth = minimumGroundPlaneWidth
            newDepth = minimumGroundPlaneDepth
            newCenterX = 0f
            newCenterZ = 0f
        } else {
            newWidth = maxOf(minimumGroundPlaneWidth, (maxX - minX) + groundPlanePadding * 2f)
            newDepth = maxOf(minimumGroundPlaneDepth, (maxZ - minZ) + groundPlanePadding * 2f)
            newCenterX = (minX + maxX) * 0.5f
            newCenterZ = (minZ + maxZ) * 0.5f
        }

        if (!force &&
            ::ground.isInitialized &&
            kotlin.math.abs(newWidth - groundWidth) < 0.01f &&
            kotlin.math.abs(newDepth - groundDepth) < 0.01f &&
            groundCenter.epsilonEquals(newCenterX, groundPlaneY, newCenterZ, 0.01f)
        ) {
            return
        }

        if (::groundModel.isInitialized) {
            groundModel.dispose()
        }
        val halfWidth = newWidth * 0.5f
        val halfDepth = newDepth * 0.5f
        val material = Material(ColorAttribute.createDiffuse(Color(0.3f,0.35f,0.3f,0.5f)))
        groundModel = modelBuilder.createRect(
            -halfWidth, 0f, -halfDepth,
            -halfWidth, 0f, halfDepth,
            halfWidth, 0f, halfDepth,
            halfWidth, 0f, -halfDepth,
            0f, 1f, 0f,
            material,
            Usage.Position.toLong() or Usage.Normal.toLong()
        )
        ground = ModelInstance(groundModel, newCenterX, groundPlaneY, newCenterZ)
        groundCenter.set(newCenterX, groundPlaneY, newCenterZ)
        groundWidth = newWidth
        groundDepth = newDepth
    }

    private fun queryCubesInScreenRect(startRaw: Vector2, endRaw: Vector2): List<Cube> {
        val minX = minOf(startRaw.x, endRaw.x)
        val maxX = maxOf(startRaw.x, endRaw.x)
        val minY = minOf(startRaw.y, endRaw.y)
        val maxY = maxOf(startRaw.y, endRaw.y)
        return scene.cubes.values.filter { cube ->
            val bounds = cube.getBoundingBox()
            val corners = arrayOf(
                Vector3(bounds.min.x, bounds.min.y, bounds.min.z),
                Vector3(bounds.min.x, bounds.min.y, bounds.max.z),
                Vector3(bounds.min.x, bounds.max.y, bounds.min.z),
                Vector3(bounds.min.x, bounds.max.y, bounds.max.z),
                Vector3(bounds.max.x, bounds.min.y, bounds.min.z),
                Vector3(bounds.max.x, bounds.min.y, bounds.max.z),
                Vector3(bounds.max.x, bounds.max.y, bounds.min.z),
                Vector3(bounds.max.x, bounds.max.y, bounds.max.z)
            )
            var projectedMinX = Float.POSITIVE_INFINITY
            var projectedMaxX = Float.NEGATIVE_INFINITY
            var projectedMinY = Float.POSITIVE_INFINITY
            var projectedMaxY = Float.NEGATIVE_INFINITY
            corners.forEach { corner ->
                val projected = activeCamera.project(Vector3(corner))
                val rawY = Gdx.graphics.height.toFloat() - projected.y
                projectedMinX = minOf(projectedMinX, projected.x)
                projectedMaxX = maxOf(projectedMaxX, projected.x)
                projectedMinY = minOf(projectedMinY, rawY)
                projectedMaxY = maxOf(projectedMaxY, rawY)
            }
            projectedMaxX >= minX &&
                projectedMinX <= maxX &&
                projectedMaxY >= minY &&
                projectedMinY <= maxY
        }
    }

    private fun pickOrbitModelPoint(screenX: Int, screenY: Int): Vector3? {
        val ray = orbitCamera.getPickRay(screenX.toFloat(), screenY.toFloat())
        val candidates = mutableListOf<Vector3>()
        val sceneHit = scene.sceneIntersectCubesRay(ray)
        if (sceneHit.hit) {
            candidates += sceneHit.point.cpy()
        }
        val guideHit = guides.sceneIntersectGuidesRay(ray)
        if (guideHit.hit) {
            candidates += guideHit.point.cpy()
        }
        val groundHit = Vector3()
        if (Intersector.intersectRayPlane(ray, Plane(Vector3.Y, 0f), groundHit)) {
            candidates += groundHit
        }
        return candidates.minByOrNull { it.dst2(ray.origin) }?.cpy()
    }

    private fun walkSupportHeightAt(x: Float, z: Float, _currentY: Float): Float {
        var support = 0f
        scene.cubes.values.forEach { cube ->
            val cubeX = floor(cube.position.x)
            val cubeZ = floor(cube.position.z)
            if (x >= cubeX && x <= cubeX + 1f && z >= cubeZ && z <= cubeZ + 1f) {
                support = maxOf(support, cube.position.y + 1f)
            }
        }
        return support
    }

    private fun uiStatusSnapshot(): VoxcraftUiOverlay.StatusSnapshot {
        val cursorText = buildString {
            append("Cursor: ")
            append(currentEvent.modelVoxel ?: "-")
            append(" | Next: ")
            append(currentEvent.modelNextVoxel ?: "-")
            append(" | Normal: ")
            append(currentEvent.normal ?: "-")
            append(" | Screen: ")
            append(currentEvent.screen ?: "-")
        }
        return VoxcraftUiOverlay.StatusSnapshot(
            fileName = filename,
            cameraMode = activeCameraMode.displayName,
            activeTool = activeTool?.name ?: "-",
            cubeCount = scene.cubes.size,
            selectionCount = selected.cubes.size,
            guideCount = guides.cubes.size,
            addMode = scene.addMode,
            message = statusMessage,
            cursor = cursorText
        )
    }

    override fun render() {
        updateActiveCamera(Gdx.graphics.deltaTime)
        rebuildGroundPlane()
        renderShadowPass()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.6f, 0.75f, 0.9f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(activeCamera)
        modelBatch.render(ground, environment)
        modelBatch.end()

        shapeRenderer.projectionMatrix = activeCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        renderGrid(
            shapeRenderer,
            activeCamera,
            Color.LIGHT_GRAY,
            Color.GRAY,
            50,
            modelSettings.gridSize.coerceAtLeast(1),
            Vector3(-0.5f, gridPlaneY, -0.5f)
        )
        drawCameraTarget()
        shapeRenderer.end()

        modelBatch.begin(activeCamera)
        modelBatch.render(scene.renderInstances(), environment)
        modelBatch.render(feedback.renderInstances(), environment)
        modelBatch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = activeCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        guides.cubes.forEach { (_:String, cub:Cube) ->
            shapeRenderer.color = cub.color
            val bb=cub.getBoundingBox()
            val pad=0.40f
            shapeRenderer.box(bb.min.x+pad,bb.min.y+pad,bb.max.z-pad,bb.width-2*pad,bb.height-2*pad,bb.depth-2*pad)
        }
        selected.cubes.forEach { (_:String, cub:Cube) ->
            shapeRenderer.color = cub.color
            val bb=cub.getBoundingBox()
            shapeRenderer.box(bb.min.x,bb.min.y,bb.max.z,bb.width,bb.height,bb.depth)
        }

        if(currentEvent.modelVoxel != null ) {
            shapeRenderer.color = Color.NAVY
            shapeRenderer.line(currentEvent.modelNextPoint, currentEvent.modelNextPoint!!.cpy().add(currentEvent.normal))
            shapeRenderer.color = Color.MAGENTA
            shapeRenderer.line(currentEvent.modelPoint, currentEvent.modelPoint!!.cpy().add(currentEvent.normal))
        }
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)

        shapeRenderer.projectionMatrix = uiOverlay.stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        activeTool?.drawScreenOverlay(shapeRenderer)
        shapeRenderer.end()

        uiOverlay.releaseScrollFocusIfPointerOutside(Gdx.input.x, Gdx.input.y)
        uiOverlay.act(Gdx.graphics.deltaTime)
        uiOverlay.draw()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderShadowPass() {
        updateShadowModelBoundsFromScene()
        updateShadowCameraFromModelBounds()
        val shadowCenter = if (shadowModelBoundsValid) shadowBoundsCenter else Vector3.Zero
        shadowLight.begin(shadowCenter, shadowLight.direction)
        shadowBatch.begin(shadowLight.camera)
        shadowBatch.render(scene.shadowInstances())
        shadowBatch.end()
        shadowLight.end()
    }

    private fun updateShadowModelBoundsFromScene() {
        if (scene.cubes.isEmpty()) {
            shadowBoundsCenter.setZero()
            shadowModelBoundsRadius = minimumShadowBoundsRadius
            shadowModelBoundsValid = false
            return
        }

        shadowBounds.inf()
        scene.cubes.values.forEach { cube ->
            shadowBounds.ext(cube.getBoundingBox())
        }
        shadowBounds.getCenter(shadowBoundsCenter)
        shadowBounds.getDimensions(shadowBoundsDimensions)
        shadowModelBoundsRadius = (shadowBoundsDimensions.len() * 0.5f).coerceAtLeast(minimumShadowBoundsRadius)
        shadowModelBoundsValid = true
    }

    private fun updateShadowCameraFromModelBounds() {
        val radius = if (shadowModelBoundsValid) {
            shadowModelBoundsRadius.coerceAtLeast(minimumShadowBoundsRadius)
        } else {
            minimumShadowBoundsRadius
        }
        val diameter = radius * 2f
        val paddedSize = (diameter * 1.15f).coerceAtLeast(minimumShadowBoundsRadius * 2f)
        val near = 0.5f
        val far = (near + diameter * 4f).coerceAtLeast(64f)
        shadowLight.setShadowVolume(paddedSize, paddedSize, near, far)
    }

    private fun drawCameraTarget() {
        val targetSize = 0.5f
        shapeRenderer.color = Color(1f, 0.9f, 0.2f, 0.8f)
        shapeRenderer.line(
            cameraTarget.x - targetSize, cameraTarget.y, cameraTarget.z,
            cameraTarget.x + targetSize, cameraTarget.y, cameraTarget.z
        )
        shapeRenderer.line(
            cameraTarget.x, cameraTarget.y - targetSize, cameraTarget.z,
            cameraTarget.x, cameraTarget.y + targetSize, cameraTarget.z
        )
        shapeRenderer.line(
            cameraTarget.x, cameraTarget.y, cameraTarget.z - targetSize,
            cameraTarget.x, cameraTarget.y, cameraTarget.z + targetSize
        )
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
                anchor.x+gridSize.toFloat(), anchor.y, anchor.z+z.toFloat(),
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

    private class ResizableDirectionalShadowLight(
        shadowMapWidth: Int,
        shadowMapHeight: Int,
        viewportWidth: Float,
        viewportHeight: Float,
        near: Float,
        far: Float
    ) : DirectionalShadowLight(
        shadowMapWidth,
        shadowMapHeight,
        viewportWidth,
        viewportHeight,
        near,
        far
    ) {
        fun setShadowVolume(viewportWidth: Float, viewportHeight: Float, near: Float, far: Float) {
            val ortho = camera as? OrthographicCamera ?: return
            ortho.viewportWidth = viewportWidth
            ortho.viewportHeight = viewportHeight
            ortho.near = near
            ortho.far = far
            halfHeight = viewportHeight * 0.5f
            halfDepth = near + (far - near) * 0.5f
            ortho.update()
        }
    }

    override fun dispose() {
        saveCurrentModel(announce = false)
        modelBatch.dispose()
        shadowBatch.dispose()
        shapeRenderer.dispose()
        sphere.dispose()
        if (::groundModel.isInitialized) {
            groundModel.dispose()
        }
        scene.dispose()
        selected.dispose()
        guides.dispose()
        feedback.dispose()
        shadowLight.dispose()
        uiOverlay.dispose()

        if (Gdx.input.inputProcessor === inputProcessors) {
            Gdx.input.inputProcessor = null
        }
        val text = tools.flatMap { tool -> tool.commands }.joinToString("\n")
        appendTextFile("$filename.mccmd", text)
        Cube.disposeSharedModels()
        if (VisUI.isLoaded()) {
            VisUI.dispose()
        }
    }

    override fun resize(width: Int, height: Int) {
        orbitCamera.viewportWidth = width.toFloat()
        orbitCamera.viewportHeight = height.toFloat()
        orbitCamera.update()
        walkCamera.viewportWidth = width.toFloat()
        walkCamera.viewportHeight = height.toFloat()
        walkCamera.update()
        configureOrthoViewport(width, height)
        alignOrthographicView(activeOrthoView)
        uiOverlay.resize(width, height)
    }

}
