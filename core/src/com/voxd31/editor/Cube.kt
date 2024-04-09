package com.voxd31.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool

class RayIntersection(
    var point: Vector3,
    var normal: Vector3,
    var distance: Float,
)
class Cube (modelBuilder: ModelBuilder,var position:Vector3,var color:Color){
    var instance: ModelInstance = ModelInstance(getModel(modelBuilder,color),
        position,
    )

    companion object{
        const val DL=0.5f
        const val DR=0.5f
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
        /// instance.model.meshes.forEachIndexed{
        ///     i:Int,mesh: Mesh? ->
        ///         println("""[$i]: ${mesh}""")
        /// }
    }
    fun getModelInstance(): ModelInstance{
        return instance
    }
    fun getId() : String {
        return "{${position.x},${position.y},${position.z}}"
    }
    fun getFaceTriangles (): HashMap<String,List<Vector3>> {
        val p = position.cpy()
        val A0 = p.cpy().add(-DL,-DL,-DL) // left bottom back
        val B0 = p.cpy().add(+DR,-DL,-DL) // right bottom back
        val C0 = p.cpy().add(+DR,-DL,+DR) // right bottom front
        val D0 = p.cpy().add(-DL,-DL,+DR) // left bottom front
        val A1 = p.cpy().add(-DL,+DR,-DL) // left bottom top
        val B1 = p.cpy().add(+DR,+DR,-DL) // right bottom top
        val C1 = p.cpy().add(+DR,+DR,+DR) // right bottom top
        val D1 = p.cpy().add(-DL,+DR,+DR) // left bottom top
        return hashMapOf(
            "top" to listOf( A1,B1,D1,B1,D1,C1),
            "bottom" to listOf( A0,D0,B0,D0,B0,C0),
            "left" to listOf( A0,A1,D0,A1,D0,D1),
            "right" to listOf( B0,B1,C0,B1,C0,C1),
            "back" to listOf( A0,A1,B0,A1,B0,B1),
            "front" to listOf( C0,C1,D0,C1,D0,D1),
        )
    }
    fun getBoundingBox():BoundingBox {
        val p = position.cpy()
        return BoundingBox(p.cpy().sub(DL),p.cpy().add(DR))
    }

    fun intersectsRay(ray: Ray):ModelIntersection? {
        //return if (Intersector.intersectRayBounds(ray, this.getBoundingBox(), Vector3(1f, 1f, 1f))) {
        return if(Intersector.intersectRayBoundsFast(ray, this.getBoundingBox())){
            val intersections = hashMapOf(
                "top" to RayIntersection(Vector3(),Vector3(0f, 1f, 0f), Float.MAX_VALUE),
                "bottom" to RayIntersection(Vector3(),Vector3(0f, -1f, 0f), Float.MAX_VALUE),
                "left" to RayIntersection(Vector3(),Vector3(-1f, 0f, 0f), Float.MAX_VALUE),
                "right" to RayIntersection(Vector3(),Vector3(1f, 0f, 0f), Float.MAX_VALUE),
                "back" to RayIntersection(Vector3(),Vector3(0f, 0f, -1f), Float.MAX_VALUE),
                "front" to RayIntersection(Vector3(),Vector3(0f, 0f, 1f), Float.MAX_VALUE),
            )
            val trMap = this.getFaceTriangles()
            val intersects = hashMapOf<String, Boolean>()
            intersects["top"] = Intersector.intersectRayTriangles(ray, trMap["top"], intersections["top"]!!.point)
            intersects["bottom"] =
                Intersector.intersectRayTriangles(ray, trMap["bottom"], intersections["bottom"]!!.point)
            intersects["left"] =
                Intersector.intersectRayTriangles(ray, trMap["left"], intersections["left"]!!.point)
            intersects["right"] =
                Intersector.intersectRayTriangles(ray, trMap["right"], intersections["right"]!!.point)
            intersects["back"] =
                Intersector.intersectRayTriangles(ray, trMap["back"], intersections["back"]!!.point)
            intersects["front"] =
                Intersector.intersectRayTriangles(ray, trMap["front"], intersections["front"]!!.point)
            intersections["top"]!!.distance = intersections["top"]!!.point.cpy().dst(ray.origin)
            intersections["bottom"]!!.distance = intersections["bottom"]!!.point.cpy().dst(ray.origin)
            intersections["left"]!!.distance = intersections["left"]!!.point.cpy().dst(ray.origin)
            intersections["right"]!!.distance = intersections["right"]!!.point.cpy().dst(ray.origin)
            intersections["back"]!!.distance = intersections["back"]!!.point.cpy().dst(ray.origin)
            intersections["front"]!!.distance = intersections["front"]!!.point.cpy().dst(ray.origin)

            var min_val = Float.MAX_VALUE
            var face_index = "not_found"
            for ((name, pair) in intersections) {
                if (intersects[name] == true) {
                    if (pair.distance <= min_val) {
                        face_index = name
                        min_val = pair.distance
                    }
                }
            }
            if (face_index != "not_found") {
                ModelIntersection(
                    hit = true,
                    point = intersections[face_index]!!.point,
                    target = this,
                    normal = intersections[face_index]!!.normal,
                    type = face_index
                )
            } else null

        } else null
    }
}