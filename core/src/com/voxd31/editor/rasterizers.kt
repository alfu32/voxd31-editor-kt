package com.voxd31.editor

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import kotlin.math.ceil
import kotlin.math.floor

val VOX_LIMIT=10000
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