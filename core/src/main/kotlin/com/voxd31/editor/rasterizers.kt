package com.voxd31.editor

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import kotlin.math.*

val VOX_LIMIT=32768
fun voxelRangeVolume(start: Vector3,end:Vector3,callback: (p:Vector3)->Unit):Unit{
    val bb=BoundingBox(start,end)

    var c =0
    for (x in floor(bb.min.x).toInt()..ceil(bb.max.x).toInt()){
        for (y in floor(bb.min.y).toInt()..ceil(bb.max.y).toInt()){
            for (z in floor(bb.min.z).toInt()..ceil(bb.max.z).toInt()){
                callback(Vector3(x.toFloat(),y.toFloat(),z.toFloat()))
                c++
                if(c > VOX_LIMIT){
                    return
                }
            }
        }
    }
}
fun voxelRangeSegment(a: Vector3,b:Vector3,callback: (p:Vector3)->Unit){
    val ab=b.cpy().sub(a)
    voxelRangeVolume(a,b){p ->
        val ap = p.cpy().sub(a)
        val dotABAP = ab.dot(ap)
        val dotABAB = ab.dot(ab)
        val t=dotABAP/dotABAB
        val p0=Vector3(
        a.x + t * ab.x,
        a.y + t * ab.y,
        a.z + t * ab.z,
        )
        if( p0.dst2(p) < ( 0.630*0.630 ) ) {
            callback(p)
        }
    }
}
fun voxelRangeSphere(a: Vector3, b:Vector3, callback: (p:Vector3)->Unit){
    val r=b.cpy().sub(a).len()
    val r2=b.cpy().sub(a).len2()
    val s=a.cpy().sub(r,r,r)
    val e=a.cpy().add(r,r,r)
    voxelRangeVolume(s,e){p ->
        if( p.dst2(a) <= r2 ) {
            callback(p)
        }
    }
}
fun voxelRangeCloudSphere(a: Vector3, b:Vector3, callback: (p:Vector3)->Unit){
    val epsilon = 0.630*0.630
    val r=b.cpy().sub(a).len()
    val r2=b.cpy().sub(a).len2()
    val s=a.cpy().sub(r,r,r)
    val e=a.cpy().add(r,r,r)
    voxelRangeVolume(s,e){p ->
        if( abs(p.dst2(a) - r2) < 1 ) {
            callback(p)
        }
    }
}
fun voxelRangeHollowSphere(a: Vector3, b:Vector3, callback: (p:Vector3)->Unit){
    val epsilon = 0.630*0.630
    val r=b.cpy().sub(a).len()
    val r2=b.cpy().sub(a).len2()
    val s=a.cpy().sub(r,r,r)
    val e=a.cpy().add(r,r,r)
    voxelRangeVolume(s,e){p ->
        if( abs(p.dst(a) - r) < 1 ) {
            callback(p)
        }
    }
}
fun voxelRangeShell(a: Vector3,b:Vector3,callback: (p:Vector3)->Unit){
    voxelRangeVolume(Vector3(a.x,a.y,a.z),Vector3(a.x,b.y,b.z),callback);
    voxelRangeVolume(Vector3(b.x,a.y,a.z),Vector3(b.x,b.y,b.z),callback);

    voxelRangeVolume(Vector3(a.x,a.y,a.z),Vector3(b.x,a.y,b.z),callback);
    voxelRangeVolume(Vector3(a.x,b.y,a.z),Vector3(b.x,b.y,b.z),callback);

    voxelRangeVolume(Vector3(a.x,a.y,a.z),Vector3(b.x,b.y,a.z),callback);
    voxelRangeVolume(Vector3(a.x,a.y,b.z),Vector3(b.x,b.y,b.z),callback);
}
fun voxelRangeFrame(a: Vector3,b:Vector3,callback: (p:Vector3)->Unit){
    voxelRangeVolume(a, Vector3(b.x, a.y, a.z),callback);
    voxelRangeVolume(a, Vector3(a.x, b.y, a.z),callback);
    voxelRangeVolume(Vector3(b.x, a.y, a.z), Vector3(b.x, b.y, a.z),callback);
    voxelRangeVolume(Vector3(a.x, b.y, a.z), Vector3(b.x, b.y, a.z),callback);

    // Generate edges for opposite face
    voxelRangeVolume(Vector3(a.x, a.y, b.z), Vector3(b.x, a.y, b.z),callback);
    voxelRangeVolume(Vector3(a.x, a.y, b.z), Vector3(a.x, b.y, b.z),callback);
    voxelRangeVolume(Vector3(b.x, a.y, b.z), b,callback);
    voxelRangeVolume(Vector3(a.x, b.y, b.z), b,callback);

    // Generate edges connecting both faces
    voxelRangeVolume(a, Vector3(a.x, a.y, b.z),callback);
    voxelRangeVolume(Vector3(b.x, a.y, a.z), Vector3(b.x, a.y, b.z),callback);
    voxelRangeVolume(Vector3(a.x, b.y, a.z), Vector3(a.x, b.y, b.z),callback);
    voxelRangeVolume(Vector3(b.x, b.y, a.z), b,callback);
}
fun voxelRangePlane(a: Vector3,b:Vector3,c:Vector3,callback: (p:Vector3)->Unit){
    val ac=c.cpy().sub(a)
    voxelRangeSegment(a,b){ v ->
        val vac = v.cpy().add(ac)
        voxelRangeSegment(v,vac,callback)
    }
}
fun voxelRangeCircle(a: Vector3,b:Vector3,callback: (p:Vector3)->Unit){
    val ab=b.cpy().sub(a)
    var radius=ab.len()
    radius=if(radius<0.5f) 1f else radius
    val arcSteps=2*Math.PI*radius
    val angleStep=1/radius
    for ( i in 0..ceil(arcSteps).toInt()){
        val p= Vector3(a.x+radius*cos(angleStep*i), a.y,a.z+radius*sin(angleStep*i))
        callback(p)
    }
}

private val TWO_PI_F = (Math.PI * 2.0).toFloat()

private fun normalizeAngleRadians(angle: Float): Float {
    var normalized = angle % TWO_PI_F
    if (normalized < 0f) {
        normalized += TWO_PI_F
    }
    return normalized
}

private fun ccwDelta(start: Float, end: Float): Float = normalizeAngleRadians(end - start)

private fun rasterizeArcOnGround(
    center: Vector3,
    radius: Float,
    startAngle: Float,
    endAngle: Float,
    direction: Int,
    y: Float,
    callback: (p: Vector3) -> Unit
) {
    if (radius < 0.5f) {
        callback(Vector3(center.x, y, center.z))
        return
    }
    val totalAngle = if (direction >= 0) ccwDelta(startAngle, endAngle) else ccwDelta(endAngle, startAngle)
    val steps = max(1, ceil(totalAngle * radius).toInt())
    for (i in 0..steps) {
        val offset = totalAngle * (i.toFloat() / steps.toFloat())
        val angle = if (direction >= 0) startAngle + offset else startAngle - offset
        callback(Vector3(center.x + radius * cos(angle), y, center.z + radius * sin(angle)))
    }
}

fun voxelRangeArcAroundCenter(center: Vector3, start: Vector3, end: Vector3, callback: (p: Vector3) -> Unit) {
    val radius = Vector2(start.x - center.x, start.z - center.z).len()
    if (radius < 0.5f) {
        voxelRangeSegment(start, end, callback)
        return
    }
    val startAngle = atan2(start.z - center.z, start.x - center.x)
    val endAngle = atan2(end.z - center.z, end.x - center.x)
    val direction = if (ccwDelta(startAngle, endAngle) <= ccwDelta(endAngle, startAngle)) 1 else -1
    rasterizeArcOnGround(center, radius, startAngle, endAngle, direction, start.y, callback)
}

fun voxelRangeArcThroughPoints(start: Vector3, mid: Vector3, end: Vector3, callback: (p: Vector3) -> Unit) {
    val x1 = start.x
    val z1 = start.z
    val x2 = mid.x
    val z2 = mid.z
    val x3 = end.x
    val z3 = end.z
    val determinant = 2f * (x1 * (z2 - z3) + x2 * (z3 - z1) + x3 * (z1 - z2))
    if (abs(determinant) < 1e-5f) {
        voxelRangeSegment(start, mid, callback)
        voxelRangeSegment(mid, end, callback)
        return
    }

    val x1s = x1 * x1 + z1 * z1
    val x2s = x2 * x2 + z2 * z2
    val x3s = x3 * x3 + z3 * z3
    val center = Vector3(
        (x1s * (z2 - z3) + x2s * (z3 - z1) + x3s * (z1 - z2)) / determinant,
        start.y,
        (x1s * (x3 - x2) + x2s * (x1 - x3) + x3s * (x2 - x1)) / determinant
    )
    val radius = Vector2(start.x - center.x, start.z - center.z).len()
    val startAngle = atan2(start.z - center.z, start.x - center.x)
    val midAngle = atan2(mid.z - center.z, mid.x - center.x)
    val endAngle = atan2(end.z - center.z, end.x - center.x)
    val direction = if (ccwDelta(startAngle, midAngle) <= ccwDelta(startAngle, endAngle)) 1 else -1
    rasterizeArcOnGround(center, radius, startAngle, endAngle, direction, start.y, callback)
}

fun voxelRangeArc0(a: Vector3,b:Vector3,c:Vector3,callback: (p:Vector3)->Unit){
    val ac=c.cpy().sub(a)
    val ab=b.cpy().sub(a)
    val startAngle=2*Math.PI+if(ab.z<0)(Math.PI+atan2(ab.z,ab.x)).toFloat() else atan2(ab.z,ab.x)
    val endAngle=2*Math.PI+if(ac.z<0)(2*Math.PI+atan2(ac.z,ac.x)).toFloat() else atan2(ac.z,ac.x)
    var radius=ac.len()
    radius=if(radius<0.5f) 1f else radius
    val arcSteps=2*Math.PI*radius
    val angleStep=1/radius
    val startStep=startAngle*radius
    val endStep=endAngle*radius
    val si=min(startStep,endStep)
    val ei=max(startStep,endStep)
    for ( i in floor(si).toInt()..ceil(ei).toInt()){
        val p= Vector3(a.x+radius*cos(angleStep*i), a.y,a.z+radius*sin(angleStep*i))
        callback(p)
    }
}
fun voxelRangeArc1(a: Vector3,b:Vector3,c:Vector3,callback: (p:Vector3)->Unit){
    val ac=c.cpy().sub(a)
    val ab=b.cpy().sub(a)
    val startAngle=atan2(ab.z,ab.x)
    val endAngle=atan2(ac.z,ac.x)
    var radius=ac.len()
    radius=if(radius<0.5f) 1f else radius
    val arcSteps=2*Math.PI*radius
    val angleStep=1/radius
    val startStep=startAngle*radius
    val endStep=endAngle*radius
    val si=min(startStep,endStep)
    val ei=max(startStep,endStep)
    for ( i in floor(si).toInt()..ceil(ei).toInt()){
        val p= Vector3(a.x+radius*cos(angleStep*i), a.y,a.z+radius*sin(angleStep*i))
        callback(p)
    }
}
fun voxelRangeArcGenerator(direction: Int):(p1: Vector3, p2: Vector3, p3: Vector3,callback: (p:Vector3)->Unit)->Unit {
    val f = fun (p1: Vector3, p2: Vector3, p3: Vector3,callback: (p:Vector3)->Unit) {
        val center = Vector2(p1.x, p1.z)
        val radius = sqrt((p3.x - p1.x).pow(2) + (p3.z - p1.z).pow(2))
        var startAngle = atan2((p2.z - p1.z).toFloat(), (p2.x - p1.x).toFloat())
        var endAngle = atan2((p3.z - p1.z).toFloat(), (p3.x - p1.x).toFloat())

        // Normalize angles to ensure they are between 0 and 2*PI
        startAngle = (startAngle + 2 * Math.PI).toFloat() // % (2 * Math.PI)
        endAngle = (endAngle + 2 * Math.PI).toFloat() // % (2 * Math.PI)

        // Adjust start and end angles based on direction
        if (direction == 1 && startAngle > endAngle) {
            endAngle += (2 * Math.PI).toFloat()
        } else if (direction == -1 && startAngle < endAngle) {
            startAngle += (2 * Math.PI).toFloat()
        }

        val points = mutableListOf<Vector2>()
        val arcLength = 1.0f // Desired distance between points along the arc
        val totalAngle = abs(endAngle - startAngle) // Total angle covered
        val angleStep =
            arcLength / radius * direction // Step in radians for each arc length, positive or negative based on direction
        val numPoints = (totalAngle / abs(angleStep)).toInt() // Calculate the number of points

        // Use a for loop to iterate over the number of points
        for (i in 0..numPoints) {
            val currentAngle = startAngle + i * angleStep
            val px = center.x + radius * cos(currentAngle).toFloat()
            val py = center.y + radius * sin(currentAngle).toFloat()
            callback(Vector3(px,p1.y, py))
        }
    }
    return f
}
