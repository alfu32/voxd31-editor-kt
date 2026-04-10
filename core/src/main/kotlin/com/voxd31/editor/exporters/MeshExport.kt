package com.voxd31.editor.exporters

import com.badlogic.gdx.math.Vector3
import com.voxd31.editor.ModelSettings
import com.voxd31.editor.SceneController

data class MeshExportOption(
    val label: String,
    val extensions: List<String>,
    val defaultExtension: String,
    val format: MeshIo.ExportFormat
)

val meshExportOptions = listOf(
    MeshExportOption("OBJ (*.obj)", listOf("obj"), "obj", MeshIo.ExportFormat.OBJ),
    MeshExportOption("STL Binary (*.stl, *.stlb)", listOf("stl", "stlb"), "stl", MeshIo.ExportFormat.STL_BINARY),
    MeshExportOption("STL ASCII (*.stla)", listOf("stla"), "stla", MeshIo.ExportFormat.STL_ASCII),
    MeshExportOption("glTF 2.0 (*.gltf)", listOf("gltf"), "gltf", MeshIo.ExportFormat.GLTF),
    MeshExportOption("GLB (*.glb)", listOf("glb"), "glb", MeshIo.ExportFormat.GLB),
    MeshExportOption("DAE/Collada (*.dae)", listOf("dae"), "dae", MeshIo.ExportFormat.DAE),
    MeshExportOption("DXF LINE + 3DFACE (*.dxf)", listOf("dxf"), "dxf", MeshIo.ExportFormat.DXF),
    MeshExportOption("3MF (*.3mf)", listOf("3mf"), "3mf", MeshIo.ExportFormat.THREE_MF),
    MeshExportOption("AMF (*.amf)", listOf("amf"), "amf", MeshIo.ExportFormat.AMF),
    MeshExportOption("FBX ASCII (*.fbx)", listOf("fbx"), "fbx", MeshIo.ExportFormat.FBX)
)

fun meshExportOptionForExtension(extension: String): MeshExportOption? {
    val normalized = extension.lowercase()
    return meshExportOptions.firstOrNull { option ->
        option.extensions.any { it == normalized }
    }
}

fun exportSceneMesh(scene: SceneController, format: MeshIo.ExportFormat, modelSettings: ModelSettings): ByteArray {
    val scale = modelSettings.unitSize.coerceAtLeast(1e-6f)
    val triangles = ArrayList<MeshIo.Triangle>(4096)
    scene.collectVisibleTriangles { a, b, c, _ ->
        triangles += MeshIo.Triangle(
            Vector3(a).scl(scale),
            Vector3(b).scl(scale),
            Vector3(c).scl(scale)
        )
    }
    return MeshIo.exportTriangles(triangles, format, meshExportSettings(modelSettings, format))
}

private fun meshExportSettings(modelSettings: ModelSettings, format: MeshIo.ExportFormat): MeshIo.ExportSettings {
    if (format != MeshIo.ExportFormat.THREE_MF) {
        return MeshIo.ExportSettings()
    }
    return MeshIo.ExportSettings(
        threeMf = MeshIo.ThreeMfExportSettings(
            unit = resolveThreeMfUnit(modelSettings.unitSuffix),
            coordinateScale = 1f
        )
    )
}

private fun resolveThreeMfUnit(unitSuffix: String): MeshIo.ThreeMfUnit {
    return when (unitSuffix.trim().lowercase()) {
        "micron", "microns", "um", "μm", "µm" -> MeshIo.ThreeMfUnit.MICRON
        "mm", "millimeter", "millimeters", "millimetre", "millimetres" -> MeshIo.ThreeMfUnit.MILLIMETER
        "cm", "centimeter", "centimeters", "centimetre", "centimetres" -> MeshIo.ThreeMfUnit.CENTIMETER
        "m", "meter", "meters", "metre", "metres" -> MeshIo.ThreeMfUnit.METER
        "in", "inch", "inches", "\"" -> MeshIo.ThreeMfUnit.INCH
        "ft", "foot", "feet", "'" -> MeshIo.ThreeMfUnit.FOOT
        else -> MeshIo.ThreeMfUnit.MILLIMETER
    }
}
