package com.xovd3i.editor

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.voxd31.editor.*
import com.voxd31.editor.exporters.readCubesCsv
import com.voxd31.editor.exporters.saveCubesAsCsv


class Voxd31Editor(val filename:String="default.vxdi") : ApplicationAdapter() {
    private val GNDSZ=100f
    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var shadowBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var scene: SceneController
    private lateinit var guides: SceneController
    private lateinit var feedback: SceneController
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var ground: ModelInstance
    private lateinit var sphere: Model
    private lateinit var inputProcessors: CompositeInputProcessor
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var shapeRenderer2d: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var currentEvent: Event

    val tools: MutableList<EditorTool> = mutableListOf() // Map activation keys to tools
    var activeTool: EditorTool? = null
    var activeToolIndex = 0
    lateinit var inputEventDispatcher: InputEventDispatcher


    private lateinit var cameraController: CameraInputController


    fun addTool(tool: EditorTool) {
        tools.add(tool)
    }

    override fun create() {
        font = BitmapFont() // This will use libGDX's default Arial font.
        spriteBatch = SpriteBatch()

        camera = PerspectiveCamera(45f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
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


        modelBuilder = ModelBuilder()
        scene = SceneController(modelBuilder)
        readCubesCsv(filename) { v:Vector3,c:Color ->
            scene.addCube(v,c)
        }
        guides = SceneController(modelBuilder)
        feedback = SceneController(modelBuilder)
        feedback.currentColor = Color.GREEN

        val colors = arrayOf(Color.WHITE,Color.GRAY,Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
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

        cameraController = EditorCameraController(camera)
        tools.add(EditorTool.VoxelEditor(scene,feedback))
        tools.add(EditorTool.makeTwoInputEditor("Volume",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeVolume(s,e,op)
        })
        tools.add(EditorTool.makeTwoInputEditor("Segment",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeSegment(s,e,op)
        })
        tools.add(EditorTool.makeTwoInputEditor("Shell",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeShell(s,e,op)
        })
        tools.add(EditorTool.makeTwoInputEditor("Frame",scene,feedback){ s:Vector3,e:Vector3,op:(p:Vector3)->Unit ->
            voxelRangeFrame(s,e,op)
        })
        tools.add(EditorTool.PlaneEditor(scene,feedback))
        activeTool = tools[activeToolIndex]

        inputEventDispatcher = InputEventDispatcher(scene,camera)
        inputProcessors= CompositeInputProcessor()
        inputProcessors.addInputProcessor(cameraController)
        inputProcessors.addInputProcessor(inputEventDispatcher)

        Gdx.input.inputProcessor = inputProcessors
        inputEventDispatcher.on("keyUp"){event ->
            when(event.keyCode){
                Input.Keys.T -> {
                    if(tools.size > 0) {
                        activeToolIndex=(activeToolIndex + 1) % tools.size
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")
                        activeTool = tools[activeToolIndex]
                        println("active tool : ${activeTool?.name} ( $activeToolIndex/${tools.size} )")
                    }
                }
                else -> {
                    println("key up : ${event.keyCode}")
                }
            }
        }
        inputEventDispatcher.on("mouseMoved"){event ->
            activeTool?.onMove?.let { it(activeTool!!,event) }
            currentEvent=event
        }
        inputEventDispatcher.on("touchUp"){event ->
            activeTool?.handleEvent(event)
            // activeTool?.onClick?.let { it(activeTool!!,event) }
            currentEvent=event
        }
        currentEvent= Event()
    }

    override fun render() {


        // Process input and update the camera
        cameraController.update()
        camera.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.5F, 0.9F, 0.9F, 1F); // Set a clear color different from your UI elements
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        shadowLight.begin(Vector3.Zero, camera.direction)
        shadowBatch.begin(shadowLight.camera)
        shadowBatch.render(scene.cubes.map { (k, v) -> v.getModelInstance()})
        shadowBatch.render(feedback.cubes.map { (k,v) -> v.getModelInstance() }, environment)
            shadowBatch.render(ground)
        shadowBatch.end()
        shadowLight.end()


        modelBatch.begin(camera)
        modelBatch.render(ground,environment)
        modelBatch.end()



        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        renderGrid(
            shapeRenderer,
            camera,
            Color.LIGHT_GRAY,
            Color.GRAY,
            50,
            1,
            Vector3(-0.5f,-0.5f,-0.5f)
        )
        shapeRenderer.end()


        modelBatch.begin(camera)
        modelBatch.render(scene.cubes.map { (k,v) -> v.getModelInstance() }, environment)
        modelBatch.render(feedback.cubes.map { (k,v) -> v.getModelInstance() }, environment)
        // modelBatch.render(ModelInstance(sphere, currentEvent.modelPoint),environment)
        // modelBatch.render(ModelInstance(sphere, currentEvent.modelNextPoint),environment)
        modelBatch.end()


        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        guides.cubes.forEach { (k:String,c:Cube) ->
            shapeRenderer.color = c.color
            val bb=c.getBoundingBox()
            shapeRenderer.box(bb.min.x,bb.min.y,bb.max.z,bb.width,bb.height,bb.depth)
        }
        feedback.cubes.forEach { (k:String,c:Cube) ->
            shapeRenderer.color = c.color
            val bb=c.getBoundingBox()
            shapeRenderer.box(bb.min.x,bb.min.y,bb.max.z,bb.width,bb.height,bb.depth)
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

        shapeRenderer2d.begin(ShapeRenderer.ShapeType.Filled)
        for( hue in 0 .. 14) {
            val rx = 10f+hue.toFloat()*40f
            val ry = Gdx.graphics.height+120f
            val color = Color()
            color.fromHsv(hue*24.0f,1f,1f)
            if(
                currentEvent.screen != null &&
                (currentEvent.screen!!.x<(rx + 40f)) &&
                (currentEvent.screen!!.x>rx)&&
                (currentEvent.screen!!.y<60f) &&
                (currentEvent.screen!!.y>20f)
            ){
                shapeRenderer2d.rect(rx-2f,ry+2f,39f,44f,Color.BLUE,Color.BLUE,Color.BLUE,Color.BLUE)
                scene.currentColor=color
            }
            if(scene.currentColor == color){
                shapeRenderer2d.rect(rx-2f,ry+2f,39f,44f,Color.BLACK,Color.BLACK,Color.BLACK,Color.BLACK)
            }

            shapeRenderer2d.rect(rx,ry,35f,40f,color,color,color,color)
        }
        for( gs in 0 until 100 step 10) {
            val rx = 10f+gs.toFloat()*4f
            val ry = Gdx.graphics.height+70f
            val color = Color()
            color.set(gs/100f,gs/100f,gs/100f,1f)
            if(
                currentEvent.screen != null &&
                (currentEvent.screen!!.x<(rx + 40f)) &&
                (currentEvent.screen!!.x>rx)&&
                (currentEvent.screen!!.y<100f) &&
                (currentEvent.screen!!.y>60f)
            ){
                shapeRenderer2d.rect(rx-2f,ry+2f,39f,44f,Color.BLUE,Color.BLUE,Color.BLUE,Color.BLUE)
                scene.currentColor=color
            }
            if(scene.currentColor == color){
                shapeRenderer2d.rect(rx-2f,ry+2f,39f,44f,Color.BLACK,Color.BLACK,Color.BLACK,Color.BLACK)
            }
            shapeRenderer2d.rect(rx,Gdx.graphics.height+70f,35f,40f,color,color,color,color)
        }
        shapeRenderer2d.end()

        spriteBatch.begin()
        if(activeTool != null) {
            font.draw(
                spriteBatch,
                """
                    Active Tool : ${tools.mapIndexed{ i,t ->if(i == activeToolIndex) t.name.uppercase() else t.name}.joinToString(" ")} (T to cycle-through)
                """.trimIndent(),
                10f,Gdx.graphics.height.toFloat()+180f,
            ) // Draws text at the specified position.

        }
        if(currentEvent.screen != null) {
            font.draw(
                spriteBatch,
                """
                    xy:${currentEvent.screen} raw:${currentEvent.modelPoint},next:${currentEvent.modelNextPoint} int:${currentEvent.modelVoxel},next:${currentEvent.modelNextVoxel} n: ${currentEvent.normal}
                """.trimIndent(),
                10f,25f,
            ) // Draws text at the specified position.
        }
        spriteBatch.end()
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
    }

    override fun resize(width: Int, height: Int) {
        // Update the camera with the new window size
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

}