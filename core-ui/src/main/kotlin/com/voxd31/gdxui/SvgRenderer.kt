package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2

val MxIdentity=floatArrayOf(1f,0f,0f,0f  , 0f,1f,0f,0f  , 0f,0f,1f,0f  ,  0f,0f,0f,1f)

class SvgRenderer: ShapeRenderer() {
    var autoShapeType: Boolean = false
    var currentColor: Color?=Color.WHITE
    var projectionMatrix: Matrix4=Matrix4().set( MxIdentity)
    var transfomationMatrix: Matrix4=Matrix4().set(MxIdentity)

    var svg=mutableListOf<String>()

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
        super.setProjectionMatrix(matrix)
    }

    override fun getProjectionMatrix(): Matrix4 {
        return projectionMatrix
        //return super.getProjectionMatrix()
    }

    override fun setTransformMatrix(matrix: Matrix4?) {
        transfomationMatrix.set(matrix)
        super.setTransformMatrix(matrix)
    }

    override fun getTransformMatrix(): Matrix4 {
        return transfomationMatrix
        return super.getTransformMatrix()
    }

    override fun identity() {

        super.identity()
    }

    override fun translate(x: Float, y: Float, z: Float) {
        transfomationMatrix.translate(x, y, z)
        super.translate(x, y, z)
    }

    override fun rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float) {
        transfomationMatrix.rotate(axisX, axisY, axisZ, degrees)
        super.rotate(axisX, axisY, axisZ, degrees)
    }

    override fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        transfomationMatrix.scale(scaleX, scaleY, scaleZ)
        super.scale(scaleX, scaleY, scaleZ)
    }

    override fun setAutoShapeType(autoShapeType: Boolean) {
        this.autoShapeType = autoShapeType
        super.setAutoShapeType(autoShapeType)
    }

    override fun begin() {
        svg= mutableListOf()
        super.begin()
    }

    override fun begin(type: ShapeType?) {
        svg= mutableListOf()
        super.begin(type)
    }

    override fun set(type: ShapeType?) {
        super.set(type)
    }

    override fun point(x: Float, y: Float, z: Float) {
        super.point(x, y, z)
    }

    override fun line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, c1: Color?, c2: Color?) {
        svg.add("""<line x0="$x" y0="$z" x1="$x2" y1="$z2" line-color="${c1}"/>""")
        super.line(x, y, z, x2, y2, z2, c1, c2)
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
        super.curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments)
    }

    override fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        super.triangle(x1, y1, x2, y2, x3, y3)
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
        super.triangle(x1, y1, x2, y2, x3, y3, col1, col2, col3)
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float) {
        super.rect(x, y, width, height)
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
        super.rect(x, y, width, height, col1, col2, col3, col4)
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
        super.rect(x, y, originX, originY, width, height, scaleX, scaleY, degrees)
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
        super.rect(x, y, originX, originY, width, height, scaleX, scaleY, degrees, col1, col2, col3, col4)
    }

    override fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        super.rectLine(x1, y1, x2, y2, width)
    }

    override fun rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, c1: Color?, c2: Color?) {
        super.rectLine(x1, y1, x2, y2, width, c1, c2)
    }

    override fun rectLine(p1: Vector2?, p2: Vector2?, width: Float) {
        super.rectLine(p1, p2, width)
    }

    override fun box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float) {
        super.box(x, y, z, width, height, depth)
    }

    override fun x(x: Float, y: Float, size: Float) {
        super.x(x, y, size)
    }

    override fun x(p: Vector2?, size: Float) {
        super.x(p, size)
    }

    override fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float) {
        super.arc(x, y, radius, start, degrees)
    }

    override fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float, segments: Int) {
        super.arc(x, y, radius, start, degrees, segments)
    }

    override fun circle(x: Float, y: Float, radius: Float) {
        super.circle(x, y, radius)
    }

    override fun circle(x: Float, y: Float, radius: Float, segments: Int) {
        super.circle(x, y, radius, segments)
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float) {
        super.ellipse(x, y, width, height)
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, segments: Int) {
        super.ellipse(x, y, width, height, segments)
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float) {
        super.ellipse(x, y, width, height, rotation)
    }

    override fun ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float, segments: Int) {
        super.ellipse(x, y, width, height, rotation, segments)
    }

    override fun cone(x: Float, y: Float, z: Float, radius: Float, height: Float) {
        super.cone(x, y, z, radius, height)
    }

    override fun cone(x: Float, y: Float, z: Float, radius: Float, height: Float, segments: Int) {
        super.cone(x, y, z, radius, height, segments)
    }

    override fun polygon(vertices: FloatArray?, offset: Int, count: Int) {
        super.polygon(vertices, offset, count)
    }

    override fun polygon(vertices: FloatArray?) {
        super.polygon(vertices)
    }

    override fun polyline(vertices: FloatArray?, offset: Int, count: Int) {
        super.polyline(vertices, offset, count)
    }

    override fun polyline(vertices: FloatArray?) {
        super.polyline(vertices)
    }

    override fun end() {
        super.end()
    }

    override fun flush() {
        super.flush()
    }

    override fun getCurrentType(): ShapeType {
        return super.getCurrentType()
    }

    override fun getRenderer(): ImmediateModeRenderer {
        return super.getRenderer()
    }

    override fun isDrawing(): Boolean {
        return super.isDrawing()
    }
}