package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.random.Random


class Cube (modelBuilder: ModelBuilder,var position:Vector3,var color:Color){
    lateinit var instance: ModelInstance
    companion object{
        val models: HashMap<Color,Model> = hashMapOf()
        fun getModel(modelBuilder: ModelBuilder,color: Color):Model {
            if(!models.contains(color)){
                val material = Material(ColorAttribute.createDiffuse(color))
                val model = modelBuilder.createBox(1f, 1f, 1f, material, VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong())
                models[color]=model
            }
            return models[color]!!

        }
    }
    init {
        instance = ModelInstance(getModel(modelBuilder,color), position.x - 0.5f, position.y - 0.5f, position.z - 0.5f)
        instance.model.meshes.forEach{
            mesh: Mesh? ->
                println(mesh)
        }
    }
    fun getModelInstance(modelBuilder: ModelBuilder): ModelInstance{
        return instance
    }
    fun getId() : String {
        return "{${position.x.toInt()},${position.y.toInt()},${position.z.toInt()}}"
    }
    fun getFaceTriangles (): HashMap<String,List<Vector3>> {
        val s=0.5f
        val p = position.cpy()
        val A0 = p.cpy().add(-s,-s,-s) // left bottom back
        val B0 = p.cpy().add(+s,-s,-s) // right bottom back
        val C0 = p.cpy().add(+s,-s,+s) // right bottom front
        val D0 = p.cpy().add(-s,-s,+s) // left bottom front
        val A1 = p.cpy().add(-s,+s,-s) // left bottom top
        val B1 = p.cpy().add(+s,+s,-s) // right bottom top
        val C1 = p.cpy().add(+s,+s,+s) // right bottom top
        val D1 = p.cpy().add(-s,+s,+s) // left bottom top
        return hashMapOf(
            "top" to listOf( A1,B1,D1,B1,D1,C1),
            "bottom" to listOf( A0,B0,D0,B0,D0,C0),
            "left" to listOf( A0,A1,D0,A1,D0,D1),
            "right" to listOf( B0,B1,C0,B1,C0,C1),
            "back" to listOf( A0,A1,B0,A1,B0,B1),
            "front" to listOf( C0,C1,D0,C1,D0,D1),
        )
    }
}