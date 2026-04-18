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

private data class ResolvedExportUnit(
    val threeMf: MeshIo.ThreeMfUnit?,
    val colladaName: String?,
    val meterScale: Float?,
    val amfName: String?,
    val dxfInsUnits: Int?
)

private fun meshExportSettings(modelSettings: ModelSettings, format: MeshIo.ExportFormat): MeshIo.ExportSettings {
    val unit = resolveExportUnit(modelSettings.unitSuffix)
    return MeshIo.ExportSettings(
        threeMf = MeshIo.ThreeMfExportSettings(
            unit = if (format == MeshIo.ExportFormat.THREE_MF) unit.threeMf else null,
            coordinateScale = 1f
        ),
        unit = MeshIo.UnitExportSettings(
            colladaName = unit.colladaName,
            meterScale = unit.meterScale,
            amfName = unit.amfName,
            dxfInsUnits = unit.dxfInsUnits
        )
    )
}

private fun resolveExportUnit(unitSuffix: String): ResolvedExportUnit {
    return when (unitSuffix.trim().lowercase()) {
        "micron", "microns", "um", "μm", "µm" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.MICRON, "micron", 0.000001f, "micron", 13)
        "mm", "millimeter", "millimeters", "millimetre", "millimetres" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.MILLIMETER, "millimeter", 0.001f, "millimeter", 4)
        "cm", "centimeter", "centimeters", "centimetre", "centimetres" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.CENTIMETER, "centimeter", 0.01f, null, 5)
        "m", "meter", "meters", "metre", "metres" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.METER, "meter", 1f, "meter", 6)
        "in", "inch", "inches", "\"" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.INCH, "inch", 0.0254f, "inch", 1)
        "ft", "foot", "feet", "'" -> ResolvedExportUnit(MeshIo.ThreeMfUnit.FOOT, "foot", 0.3048f, "feet", 2)
        else -> ResolvedExportUnit(null, null, null, null, null)
    }
}
