package com.xovd3i.editor

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.voxd31.editor.SceneController
import kotlin.random.Random

class Voxd31Editor : ApplicationAdapter() {
    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var shadowBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var scene: SceneController
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var ground: ModelInstance


    private lateinit var cameraController: CameraInputController

    override fun create() {
        camera = PerspectiveCamera(75f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 300f
            update()
        }


        modelBatch = ModelBatch()
        shadowBatch = ModelBatch(DepthShaderProvider())

        environment = Environment()
        shadowLight = DirectionalShadowLight(2048, 2048, 60f, 60f, 1f, 300f).apply {
            set(0.5f, 0.5f, 0.5f, -1f, -1.8f, -1.2f)
            environment.add(this)
            environment.shadowMap = this
        }
        environment.add(DirectionalLight().set(0.5f, 0.5f, 0.5f, -1f, -1.8f, -1.2f))
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f)) // Reduced ambient light


        modelBuilder = ModelBuilder()
        scene = SceneController(modelBuilder,camera)

        val colors = arrayOf(Color.WHITE,Color.GRAY,Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
        // Create 20 cubes with random positions and colors
        for (i in 1..20) {
            val x = MathUtils.random(-5, 5).toFloat()
            val y = MathUtils.random(0,2).toFloat()
            val z = MathUtils.random(-5, 5).toFloat()
            scene.currentColor=colors[MathUtils.random(0,colors.size-1)]
            scene.addCube(x,y,z)
        }
        val matGround = Material(ColorAttribute.createDiffuse(Color.GRAY))
        val groundBox = modelBuilder.createBox(20f, 0.1f, 20f, matGround, Usage.Position.toLong() or Usage.Normal.toLong())

        ground = (ModelInstance(groundBox, 0f,-0.55f,0f))



        cameraController = CameraInputController(camera).apply {
            target.set(0f, 0f, 0f) // Set the target point the camera orbits around
            autoUpdate = true // Automatically update the camera position based on input
        }
        Gdx.input.inputProcessor = cameraController
    }

    override fun render() {

        // Process input and update the camera
        cameraController.update()

        shadowLight.begin(Vector3.Zero, camera.direction)
        shadowBatch.begin(shadowLight.camera)
            scene.cubes.map{it.instance}.forEach { shadowBatch.render(it) }
            shadowBatch.render(ground)
        shadowBatch.end()
        shadowLight.end()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera.update()
        modelBatch.begin(camera)
        modelBatch.render(ground,environment)
        modelBatch.render(scene.cubes.map { it.instance }, environment)
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        shadowBatch.dispose()
        scene.dispose()
        shadowLight.dispose()

        // If you set a different input processor later, you might need to do this
        if (Gdx.input.inputProcessor == cameraController) {
            Gdx.input.inputProcessor = null
        }
    }

    override fun resize(width: Int, height: Int) {
        // Update the camera with the new window size
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

}