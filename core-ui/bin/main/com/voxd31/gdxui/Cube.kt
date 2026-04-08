package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray


class RayIntersection(
    var point: Vector3,
    var normal: Vector3,
    var distance: Float,
)
class Cube (val modelBuilder: ModelBuilder,var position:Vector3,var color:Color){
    var instance: ModelInstance = ModelInstance(
        getModel(modelBuilder, color),
        position,
    )

    companion object{
        const val DL=0.5f//0.4999999f
        const val DR=0.5f//0.4999999f
        val models: HashMap<Color,Model> = hashMapOf()
        fun getModel(modelBuilder: ModelBuilder,color: Color):Model {
            if(!models.contains(color)){
                val material = Material(ColorAttribute.createDiffuse(color.r,color.g,color.b,color.a))
                if(color.a < 0.99) {
                    material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, color.a))
                }
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
    fun getIntId() : String {
        return "{${position.x.toInt()},${position.y.toInt()},${position.z.toInt()}}"
    }
    fun getNeighbouringPositions(): List<Cube> {
        val p = position.cpy()
        val l = mutableListOf<Cube>()
        for (addx in -1..1){
            for (addy in -1..1){
                for (addz in -1..1){
                    l.add(Cube(modelBuilder,p.cpy().add(addx.toFloat(),addy.toFloat(),addz.toFloat()),this.color.cpy()))
                }
            }
        }
        return l
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
    fun projectVectorOntoVector(vectorToProject: Vector3, targetVector: Vector3): Vector3 {
        val dotProduct = vectorToProject.dot(targetVector)
        val magnitudeSquared = targetVector.len2()
        val scalarProjection = dotProduct / magnitudeSquared
        return Vector3(targetVector).cpy().scl(scalarProjection)
    }
    fun intersectsGuidesRay(ray: Ray): ModelIntersection? {
        val localAxes=mutableListOf(Vector3.X,Vector3.Y,Vector3.Z)
        val intersection = localAxes
            .flatMap{ normal ->
                val intersection = Vector3.Zero

                if(Intersector.intersectRayPlane(ray, Plane(normal,position),intersection)){
                    localAxes.map { ax ->
                        val comp = projectVectorOntoVector(intersection.cpy().sub(position),ax)
                        ModelIntersection(
                            hit = true,
                            point = comp.cpy().add(position),
                            normal = ax.cpy(),
                            target = Cube(modelBuilder = ModelBuilder(),position = comp.cpy().add(position), color = Color.CYAN),
                            type = "guide",
                        )
                    }.filter{ mi -> mi.point.epsilonEquals(position,0.01f)}
                } else {
                    listOf()
                }
            }
            .minByOrNull{mi -> -mi.point.dst2(ray.origin)}
        return intersection
    }


    fun findClosestPoints(ray1: Ray, ray2: Ray): Vector3? {
        val p1 = ray1.origin
        val p2 = ray2.origin
        val d1 = Vector3().set(ray1.direction).nor() // Normalize to ensure we're working with unit vectors
        val d2 = Vector3().set(ray2.direction).nor() // Normalize

        val n = Vector3().set(d1).crs(d2) // Cross product to find a vector normal to the plane containing d1 and d2
        val n1 = Vector3().set(d2).crs(n) // Cross product to find the normal to the plane containing p2, d2, and n
        val n2 = Vector3().set(n).crs(d1) // Cross product to find the normal to the plane containing p1, d1, and n

        val c1 = (p2.cpy().sub(p1).dot(n2)) / (d1.dot(n2))
        val c2 = (p1.cpy().add(d1.scl(c1)).sub(p2).dot(n1)) / (d2.dot(n1))

        val closestPointOnRay1 = p1.cpy().add(d1.cpy().scl(c1))
        val closestPointOnRay2 = p2.cpy().add(d2.cpy().scl(c2))

        if(closestPointOnRay1.dst2(closestPointOnRay2)<0.25) {
            println("points found between $ray1 and $ray2 -> $closestPointOnRay1 $closestPointOnRay2")
            return closestPointOnRay2
        } else {
            println("points not close enough between $ray1 and $ray2 -> $closestPointOnRay1 $closestPointOnRay2")
            return null
        }
    }

    fun copy(): Cube = Cube(
        modelBuilder = modelBuilder,
        position=position.cpy(),
        color=color.cpy(),
    )

    override fun toString(): String {
        return this.getId()
    }
}