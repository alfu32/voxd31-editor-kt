package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2

val MxIdentity=floatArrayOf(1f,0f,0f,0f  , 0f,1f,0f,0f  , 0f,0f,1f,0f  ,  0f,0f,0f,1f)

class SvgRenderer: ShapeRenderer() {
    private var autoShapeType: Boolean = false
    private var currentColor: Color?=Color.WHITE
    private var projectionMatrix: Matrix4=Matrix4().set( MxIdentity)
    private var transfomationMatrix: Matrix4=Matrix4().set(MxIdentity)
    private var shapeType = ShapeType.Line

    private var svg=mutableListOf<String>()

    override fun dispose() {
        super.dispose()
    }

    override fun setColor(color: Color?) {
        super.setColor(color)
        currentColor=color
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        super.setColor(r, g, b, a)
        currentColor=Color(r,g,b,a)
    }

    override fun getColor(): Color {
        return currentColor!!
    }

    override fun updateMatrices() {
        super.updateMatrices()
    }

    override fun setProjectionMatrix(matrix: Matrix4?) {
        projectionMatrix.set(matrix)
    }

    override fun getProjectionMatrix(): Matrix4 {
        return projectionMatrix
    }

    override fun setTransformMatrix(matrix: Matrix4?) {
        transfomationMatrix.set(matrix)
    }

    override fun getTransformMatrix(): Matrix4 {
        return transfomationMatrix
    }

    override fun identity() {
        super.identity()
    }

    override fun translate(x: Float, y: Float, z: Float) {
        transfomationMatrix.translate(x, y, z)
    }

    override fun rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float) {
        transfomationMatrix.rotate(axisX, axisY, axisZ, degrees)
    }

    override fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        transfomationMatrix.scale(scaleX, scaleY, scaleZ)
    }

    override fun setAutoShapeType(autoShapeType: Boolean) {
        this.autoShapeType = autoShapeType
    }

    override fun begin() {
        svg= mutableListOf()
        svg.add("""<g>""")
    }

    override fun begin(type: ShapeType?) {
        svg= mutableListOf()
        svg.add("""<g>""")
        this.shapeType= type ?: this.shapeType
    }

    override fun set(type: ShapeType?) {
        this.shapeType= type ?: this.shapeType
    }

    override fun point(x: Float, y: Float, z: Float) {
        //super.point(x, y, z)
    }

    override fun line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, c1: Color?, c2: Color?) {
        svg.add("""<line x0="$x" y0="$z" x1="$x2" y1="$z2" line-color="${c1}"/>""")
    }

    override fun curve(
        x1: Float,
        y1: Float,
        cx1: Float,
        cy1: Float,
        cx2: Float,
        cy2: Float,
        x2: Float,
        y2: Float,
        segments: Int
    ) {
        svg.add("""<curve/>""")
    }

    override fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        svg.add("""<triangle/>""")
    }

    override fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        col1: Color?,
        col2: Color?,
        col3: Color?
    ) {
        svg.add("""<triangle/>""")
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float) {
        svg.add("""<rect/>""")
    }

    override fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        col1: Color?,
        col2: Color?,
        col3: Color?,
        col4: Color?
    ) {
        svg.add("""<rect/>""")
    }

    override fun rect(
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        degrees: Float
    ) {
        svg.add("""<rect/>""")
    }

    override fun rect(
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        degrees: Float,
        col1: Color?,
        col2: Color?,
        col3: Color?,
        col4: Color?
    ) {
        svg.add("""<rect/>""")
    }

    override fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        svg.add("""<rect/>""")
    }

    override fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, c1: Color?, c2: Color?) {
        svg.add("""<rect/>""")
    }

    override fun rectLine(p1: Vector2?, p2: Vector2?, width: Float) {
        svg.add("""<rect/>""")
    }

    override fun box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        svg.add("""<rect/>""")
    }

    override fun x(x: Float, y: Float, size: Float) {
        svg.add("""<x/>""")
    }

    override fun x(p: Vector2?, size: Float) {
        svg.add("""<x/>""")
    }

    override fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float) {
        svg.add("""<arc/>""")
    }

    override fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float, segments: Int) {
        svg.add("""<arc/>""")
    }

    override fun circle(x: Float, y: Float, radius: Float) {
        svg.add("""<circle/>""")
    }

    override fun circle(x: Float, y: Float, radius: Float, segments: Int) {
        svg.add("""<circle/>""")
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float) {
        svg.add("""<ellipse/>""")
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, segments: Int) {
        svg.add("""<ellipse/>""")
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float) {
        svg.add("""<ellipse/>""")
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float, segments: Int) {
        svg.add("""<ellipse/>""")
    }

    override fun cone(x: Float, y: Float, z: Float, radius: Float, height: Float) {
        svg.add("""<cone/>""")
    }

    override fun cone(x: Float, y: Float, z: Float, radius: Float, height: Float, segments: Int) {
        svg.add("""<cone/>""")
    }

    override fun polygon(vertices: FloatArray?, offset: Int, count: Int) {
        svg.add("""<poly/>""")
    }

    override fun polygon(vertices: FloatArray?) {
        svg.add("""<poly/>""")
    }

    override fun polyline(vertices: FloatArray?, offset: Int, count: Int) {
        svg.add("""<poly/>""")
    }

    override fun polyline(vertices: FloatArray?) {
        svg.add("""<poly/>""")
    }

    override fun end() {
        svg.add("""</g>""")
    }

    override fun flush() {
        //super.flush()
    }


    override fun getRenderer(): ImmediateModeRenderer {
        return SvgImmediateModeRenderer()
    }

    override fun isDrawing(): Boolean {
        return true
    }
}

class SvgImmediateModeRenderer: ImmediateModeRenderer{
    override fun begin(p0: Matrix4?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun color(p0: Color?) {
        TODO("Not yet implemented")
    }

    override fun color(p0: Float, p1: Float, p2: Float, p3: Float) {
        TODO("Not yet implemented")
    }

    override fun color(p0: Float) {
        TODO("Not yet implemented")
    }

    override fun texCoord(p0: Float, p1: Float) {
        TODO("Not yet implemented")
    }

    override fun normal(p0: Float, p1: Float, p2: Float) {
        TODO("Not yet implemented")
    }

    override fun vertex(p0: Float, p1: Float, p2: Float) {
        TODO("Not yet implemented")
    }

    override fun end() {
        TODO("Not yet implemented")
    }

    override fun getNumVertices(): Int {
        TODO("Not yet implemented")
    }

    override fun getMaxVertices(): Int {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}