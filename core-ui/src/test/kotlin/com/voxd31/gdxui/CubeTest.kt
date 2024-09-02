package com.voxd31.gdxui

import org.mockito.Mockito.mock
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

//@RunWith(GdxTestRunner::class)
class CubeTest {
    lateinit var mb:ModelBuilder

    @BeforeEach
    fun setUp() {


        // Mock the GL20 to avoid null pointer on Gdx.gl calls.
        Gdx.gl = mock(GL20::class.java)
        Gdx.gl20=mock(GL20::class.java)
        Gdx.gl.glViewport(0, 0, 256,256)
        Gdx.gl.glClearColor(0.5F, 0.9F, 0.9F, 1F); // Set a clear color different from your UI elements
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        mb= MockModelBuilder()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun getNeigbourCubesTest() {
        val cube = Cube(mb, Vector3(1f,1f,1f),Color.RED)

        val neighbors = cube.getNeighbouringPositions()

        neighbors.forEach{ neighbor -> println(neighbor.getIntId()) }
    }

    @Test
    fun getFaceTrianglesTest() {
        val cube = Cube(mb, Vector3(1f,1f,1f),Color.RED)
        val triangles = cube.getFaceTriangles()

        triangles.forEach{ triangle -> println(""" ${triangle.key} = ${triangle.value}""") }
    }

    @Test
    fun getBoundingBoxTest() {
        val cube = Cube(mb, Vector3(1f,1f,1f),Color.RED)
        val bb = cube.getBoundingBox()

        println(bb)
    }

    @Test
    fun intersectsRayTest() {
    }

    @Test
    fun projectVectorOntoVectorTest() {
    }

    @Test
    fun intersectsGuidesRayTest() {
    }

    @Test
    fun findClosestPointsTest() {
    }

    @Test
    fun copyTest() {
    }

    @Test
    fun getModelBuilderTest() {
    }
}