package com.voxd31.editor.exporters

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.Cube
import java.io.File


fun saveCubesAsCsv(cubes:List<Cube>, filename:String) {
    val f=File(filename)
    println("saving scene to $filename in ${f.path} ( ${f.absoluteFile.path} )")
    f.bufferedWriter().use { out ->
        cubes.forEach { cube ->
            val line = "${cube.position.x};${cube.position.y};${cube.position.z};" +
                    "${cube.color.r};${cube.color.g};${cube.color.b};${cube.color.a}\n"
            out.write(line)
        }
    }
}

fun readCubesCsv(filename: String,onCubeData: (p:Vector3,c:Color)->Unit) {
    val f=File(filename)
    if(f.exists()) {
        f.bufferedReader().forEachLine { line ->
            val parts = line.split(";")
            if (parts.size >= 7) { // Ensure there are enough parts for a Cube
                val position = Vector3(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
                val color = Color(parts[3].toFloat(), parts[4].toFloat(), parts[5].toFloat(), parts[6].toFloat())
                onCubeData(position, color)
            }
        }
    } else {
        println("file $filename not found in ${f.path} ( ${f.absoluteFile.path} )")
    }
}