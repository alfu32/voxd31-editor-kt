package com.voxd31.editor.exporters

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.editor.ModelSettings
import com.voxd31.editor.SceneController
import com.voxd31.gdxui.MockModelBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class MeshExportTest {
    private lateinit var scene: SceneController

    @BeforeEach
    fun setUp() {
        scene = SceneController(MockModelBuilder())
        scene.addCube(Vector3(0f, 0f, 0f), Color.RED)
        scene.addCube(Vector3(1f, 0f, 0f), Color.BLUE)
    }

    @AfterEach
    fun tearDown() {
        scene.dispose()
    }

    @Test
    fun objExportUsesSolidShellAndWeldedVerticesAcrossColorBoundaries() {
        val text = String(
            exportSceneMesh(scene, MeshIo.ExportFormat.OBJ, ModelSettings()),
            StandardCharsets.UTF_8
        )

        val vertexCount = text.lineSequence().count { it.startsWith("v ") }
        val faceCount = text.lineSequence().count { it.startsWith("f ") }

        assertEquals(12, vertexCount)
        assertEquals(20, faceCount)
    }

    @Test
    fun amfExportUsesSolidShellAndWeldedVerticesAcrossColorBoundaries() {
        val text = String(
            exportSceneMesh(scene, MeshIo.ExportFormat.AMF, ModelSettings()),
            StandardCharsets.UTF_8
        )

        val vertexCount = Regex("<vertex><coordinates>").findAll(text).count()
        val triangleCount = Regex("<triangle><v1>").findAll(text).count()

        assertEquals(12, vertexCount)
        assertEquals(20, triangleCount)
    }
}
