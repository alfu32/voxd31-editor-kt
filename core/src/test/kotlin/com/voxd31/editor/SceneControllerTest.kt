package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.MockModelBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SceneControllerTest {
    private lateinit var scene: SceneController

    @BeforeEach
    fun setUp() {
        scene = SceneController(MockModelBuilder())
    }

    @AfterEach
    fun tearDown() {
        scene.dispose()
    }

    @Test
    fun mergesAdjacentFacesWithinChunk() {
        scene.addCube(Vector3(0f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(1f, 0f, 0f), Color.RED)

        assertEquals(1, scene.visibleRenderChunkCount())
        assertEquals(6, scene.visibleRenderFaceCount())
    }

    @Test
    fun doesNotMergeAcrossChunkBoundaries() {
        scene.addCube(Vector3(15f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(16f, 0f, 0f), Color.RED)

        assertEquals(2, scene.visibleRenderChunkCount())
        assertEquals(10, scene.visibleRenderFaceCount())
    }

    @Test
    fun doesNotMergeFacesAcrossColorBoundaries() {
        scene.addCube(Vector3(0f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(1f, 0f, 0f), Color.BLUE)

        assertEquals(1, scene.visibleRenderChunkCount())
        assertEquals(10, scene.visibleRenderFaceCount())
    }
}
