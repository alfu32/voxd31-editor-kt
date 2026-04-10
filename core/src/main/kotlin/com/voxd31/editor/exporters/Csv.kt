package com.voxd31.editor.exporters

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.editor.DefaultDocumentIoService
import com.voxd31.editor.DocumentIoService
import com.voxd31.editor.ModelSettings
import com.voxd31.gdxui.Cube

data class LoadedVoxelModel(
    val settings: ModelSettings = ModelSettings(),
    val cubes: List<Pair<Vector3, Color>> = emptyList()
)

fun saveModelAsCsv(cubes: List<Cube>, filename: String, settings: ModelSettings = ModelSettings()) {
    saveModelAsCsv(cubes, filename, settings, DefaultDocumentIoService)
}

fun saveModelAsCsv(
    cubes: List<Cube>,
    filename: String,
    settings: ModelSettings,
    documentIoService: DocumentIoService
) {
    val out = buildString {
        append("#voxcraft;gridSize;${settings.gridSize}\n")
        append("#voxcraft;unitSize;${settings.unitSize}\n")
        append("#voxcraft;unitSuffix;${settings.unitSuffix}\n")
        cubes.forEach { cube ->
            append("${cube.position.x};${cube.position.y};${cube.position.z};")
            append("${cube.color.r};${cube.color.g};${cube.color.b};${cube.color.a}\n")
        }
    }
    documentIoService.writeUtf8(filename, out, append = false)
}

fun loadModelFromCsv(filename: String): LoadedVoxelModel {
    return loadModelFromCsv(filename, DefaultDocumentIoService)
}

fun loadModelFromCsv(filename: String, documentIoService: DocumentIoService): LoadedVoxelModel {
    if (!documentIoService.exists(filename)) {
        println("file $filename not found")
        return LoadedVoxelModel()
    }

    val settings = ModelSettings()
    val cubes = mutableListOf<Pair<Vector3, Color>>()
    documentIoService.readUtf8(filename).lineSequence().forEach { line ->
        if (line.startsWith("#voxcraft;")) {
            val parts = line.split(";", limit = 3)
            if (parts.size >= 3) {
                when (parts[1]) {
                    "gridSize" -> settings.gridSize = parts[2].toIntOrNull()?.coerceAtLeast(1) ?: settings.gridSize
                    "unitSize" -> settings.unitSize = parts[2].toFloatOrNull()?.coerceAtLeast(1e-6f) ?: settings.unitSize
                    "unitSuffix" -> settings.unitSuffix = parts[2].ifBlank { settings.unitSuffix }
                }
            }
            return@forEach
        }
        val parts = line.split(";")
        if (parts.size >= 7) {
            val position = Vector3(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
            val color = Color(parts[3].toFloat(), parts[4].toFloat(), parts[5].toFloat(), parts[6].toFloat())
            cubes += position to color
        }
    }
    return LoadedVoxelModel(settings = settings, cubes = cubes)
}

fun saveCubesAsCsv(cubes: List<Cube>, filename: String) {
    saveModelAsCsv(cubes, filename, ModelSettings())
}

fun readCubesCsv(filename: String, onCubeData: (p: Vector3, c: Color) -> Unit) {
    loadModelFromCsv(filename).cubes.forEach { (position, color) ->
        onCubeData(position, color)
    }
}
