package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.MockModelBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertTrue(scene.hasConsistentFaceWinding())
    }

    @Test
    fun doesNotMergeAcrossChunkBoundaries() {
        scene.addCube(Vector3(15f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(16f, 0f, 0f), Color.RED)

        assertEquals(2, scene.visibleRenderChunkCount())
        assertEquals(10, scene.visibleRenderFaceCount())
        assertTrue(scene.hasConsistentFaceWinding())
    }

    @Test
    fun keepsSharedFacesAcrossColorBoundaries() {
        scene.addCube(Vector3(0f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(1f, 0f, 0f), Color.BLUE)

        assertEquals(1, scene.visibleRenderChunkCount())
        assertEquals(12, scene.visibleRenderFaceCount())
        assertTrue(scene.hasConsistentFaceWinding())
    }

    @Test
    fun splitsPlanarFacesToAvoidTJunctions() {
        for (x in 0..3) {
            for (z in 0..3) {
                scene.addCube(Vector3(x.toFloat(), 0f, z.toFloat()), Color.BLACK)
            }
        }
        for (x in 0..1) {
            scene.addCube(Vector3(x.toFloat(), 0f, 4f), Color.ORANGE)
        }

        assertFalse(scene.hasPlanarTJunctions())
        assertTrue(scene.hasConsistentFaceWinding())
    }

    @Test
    fun renderOutputUsesUnitGridSubdivisionAfterMerge() {
        scene.addCube(Vector3(0f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(1f, 0f, 0f), Color.RED)

        assertTrue(scene.renderFacesUseUnitGridSubdivision())
    }
}
