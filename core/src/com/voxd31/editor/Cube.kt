package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
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
        instance = ModelInstance(getModel(modelBuilder,color), position.x.toFloat()-0.5f, position.y.toFloat()-0.5f, position.z.toFloat()-0.5f)
    }
    fun getModelInstance(modelBuilder: ModelBuilder): ModelInstance{
        return instance
    }
    fun getId() : String {
        return "{${position.x.toInt()},${position.x.toInt()},${position.x.toInt()}}"
    }
}