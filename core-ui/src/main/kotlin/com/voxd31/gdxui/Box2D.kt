package com.voxd31.gdxui

import com.badlogic.gdx.math.Vector2
import kotlin.math.abs

class Box2D(
) {
    val min: Vector2 = Vector2(Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY)
    val max: Vector2 = Vector2(Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY)
    companion object{
        val tmpVector: Vector2 = Vector2()
        fun of(a:Vector2):Box2D{
            val b = Box2D()
            b.set(a.cpy(),a.cpy())
            return b
        }
        fun of(a:Box2D):Box2D{
            val b = Box2D()
            b.set(a.min.cpy(),a.max.cpy())
            return b
        }
    }
    private val cnt = Vector2(0f,0f)
    private val dim = Vector2(Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY)
    fun getCenter(): Vector2 {
        return cnt
    }
    fun getCorner000(): Vector2 {
        return Vector2(min.x, min.y)
    }

    fun getCorner001(): Vector2 {
        return Vector2(min.x, min.y)
    }

    fun getCorner010(): Vector2 {
        return Vector2(min.x, max.y)
    }

    fun getCorner011(): Vector2 {
        return Vector2(min.x, max.y)
    }
    fun getWidth(): Float {
        return dim.x
    }

    fun getHeight(): Float {
        return dim.y
    }

    fun update() {
        val x0=kotlin.math.min(max.x, min.x)
        val y0=kotlin.math.min(max.y, min.y)
        val x1=kotlin.math.max(max.x, min.x)
        val y1=kotlin.math.max(max.y, min.y)
        this.min.x = x0
        this.min.y = y0
        this.max.x = x1
        this.max.y = y1
        this.cnt.set(this.min).add(this.max).scl(0.5f)
        this.dim.set(this.max).sub(this.min)
    }

    fun inf(): Box2D {
        this.min.x=Float.POSITIVE_INFINITY
        this.min.y=Float.POSITIVE_INFINITY
        this.max.x=Float.NEGATIVE_INFINITY
        this.max.y=Float.NEGATIVE_INFINITY
        this.cnt.x=0f
        this.cnt.y=0f
        this.dim.x=0f
        this.dim.y=0f
        return this
    }
    fun set(vmin: Vector2,vmax:Vector2):Box2D{
        this.min.set(vmin)
        this.max.set(vmax)
        update()
        return this
    }

    private fun ext0(point: Vector2): Box2D {
        this.set(
            this.min.set(kotlin.math.min(this.min.x, point.x), kotlin.math.min(this.min.y, point.y),),
            this.max.set(
                kotlin.math.max(this.max.x.toDouble(), point.x.toDouble()).toFloat(),
                kotlin.math.max(this.max.y.toDouble(), point.y.toDouble()).toFloat(),
            )
        )
        return this
    }
    fun ext(point: Vector2): Box2D {
        this.ext0(point)
        update()
        return this
    }
    fun ext(b: Box2D): Box2D {
        this.ext(b.getCorner000())
        this.ext(b.getCorner001())
        this.ext(b.getCorner010())
        this.ext(b.getCorner011())
        update()
        return this
    }

    fun contains(v: Vector2): Boolean {
        return this.min.x <= v.x && (this.max.x >= v.x) && (this.min.y <= v.y) && (this.max.y >= v.y)
    }

    fun contains(b: Box2D): Boolean {
        return !this.isValid() || this.min.x <= b.min.x && (this.min.y <= b.min.y) && (this.max.x >= b.max.x) && (this.max.y >= b.max.y)
    }

    /// fun contains(obb: OrientedBoundingBox): Boolean {
    ///     return this.contains(obb.getCorner000(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner001(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner010(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner011(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner100(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner101(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner110(Box2D.tmpVector))
    ///             && this.contains(obb.getCorner111(Box2D.tmpVector))
    /// }

    fun intersects(b: Box2D): Boolean {
        if (!this.isValid()) {
            return false
        } else {
            val lx = abs((this.cnt.x - b.cnt.x).toDouble()).toFloat()
            val sumx = this.dim.x / 2.0f + b.dim.x / 2.0f
            val ly = abs((this.cnt.y - b.cnt.y).toDouble()).toFloat()
            val sumy = this.dim.y / 2.0f + b.dim.y / 2.0f
            return lx <= sumx && ly <= sumy
        }
    }

    override fun toString(): String {
        return "[" + this.min + "|" + this.max + "]"
    }

    fun clr(): Box2D {
        this.set(this.min.set(0.0f, 0.0f), this.max.set(0.0f, 0.0f))
        update()
        return this
    }

    fun isValid(): Boolean {
        return this.min.x <= this.max.x && (this.min.y <= this.max.y)
    }
}

