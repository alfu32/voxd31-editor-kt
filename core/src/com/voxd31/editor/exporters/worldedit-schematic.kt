package com.voxd31.editor.exporters

import com.badlogic.gdx.math.collision.BoundingBox
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.world.block.BlockTypes
import com.voxd31.editor.Cube
import java.io.File
import java.io.FileOutputStream

public fun generateSchematicPayload(cubes: List<Cube>): Clipboard {
    // Create a bounding box to calculate min and max points
    val box = BoundingBox()

    // Extend the bounding box to include all cube positions
    cubes.forEach { cube ->
        box.ext(cube.position)
    }

    // Convert from LibGDX Vector3 to WorldEdit BlockVector3
    val minPoint = BlockVector3.at(
        box.min.x.toInt(),
        box.min.y.toInt(),
        box.min.z.toInt()
    )
    val maxPoint = BlockVector3.at(
        box.max.x.toInt(),
        box.max.y.toInt(),
        box.max.z.toInt()
    )

    // Create a clipboard with the calculated bounds
    val clipboard = BlockArrayClipboard(CuboidRegion(minPoint,maxPoint))
    cubes.forEach { cube ->
        // Convert cube position to BlockVector3 for WorldEdit
        val position = BlockVector3.at(cube.position.x.toInt(), cube.position.y.toInt(), cube.position.z.toInt())
        clipboard.setBlock(position, BlockTypes.GRAY_WOOL!!.defaultState.toImmutableState())
    }

    return clipboard
}


public fun writeSchematicToFile(clipboard: Clipboard, outputFile: File) {
    try {
        val format = ClipboardFormats.findByFile(outputFile)
        format?.let { fmt ->
            FileOutputStream(outputFile).use { fos ->
                fmt.getWriter(outputFile.outputStream()).use { writer ->
                    writer.write(clipboard)
                }
            }
        } ?: println("Unsupported file format for output file: $outputFile")
    } catch (e: Exception) {
        println("Failed to write schematic to file: ${e.message}")
    }
}

public fun saveSchematicToFile(cubes: List<Cube>, filename: String) {
    val we = WorldEdit.getInstance()
    val cpb = generateSchematicPayload(cubes)
    writeSchematicToFile(cpb,File(filename))
}