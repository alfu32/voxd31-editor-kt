package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

class MockModelBuilder: ModelBuilder() {
    override fun begin() {
        //super.begin()
    }

    override fun end(): Model {
        return Model()
        //return super.end()
    }

    override fun node(node: Node?): Node {
        return Node()
        //return super.node(node)
    }

    override fun node(): Node {
        return Node()
        //return super.node()
    }

    override fun node(id: String?, model: Model?): Node {
        return Node()
        //return super.node(id, model)
    }

    override fun manage(disposable: Disposable?) {
        //super.manage(disposable)
    }

    override fun part(meshpart: MeshPart?, material: Material?) {
        //super.part(meshpart, material)
    }

    override fun part(
        id: String?,
        mesh: Mesh?,
        primitiveType: Int,
        offset: Int,
        size: Int,
        material: Material?
    ): MeshPart {
        return MeshPart()
        //return super.part(id, mesh, primitiveType, offset, size, material)
    }

    override fun part(id: String?, mesh: Mesh?, primitiveType: Int, material: Material?): MeshPart {
        return MeshPart()
        //return super.part(id, mesh, primitiveType, material)
    }

    override fun part(
        id: String?,
        primitiveType: Int,
        attributes: VertexAttributes?,
        material: Material?
    ): MeshPartBuilder {
        return super.part(id, primitiveType, attributes, material)
    }

    override fun part(id: String?, primitiveType: Int, attributes: Long, material: Material?): MeshPartBuilder {
        return super.part(id, primitiveType, attributes, material)
    }

    override fun createBox(width: Float, height: Float, depth: Float, material: Material?, attributes: Long): Model {
        return Model()
        //return super.createBox(width, height, depth, material, attributes)
    }

    override fun createBox(
        width: Float,
        height: Float,
        depth: Float,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createBox(width, height, depth, primitiveType, material, attributes)
    }

    override fun createRect(
        x00: Float,
        y00: Float,
        z00: Float,
        x10: Float,
        y10: Float,
        z10: Float,
        x11: Float,
        y11: Float,
        z11: Float,
        x01: Float,
        y01: Float,
        z01: Float,
        normalX: Float,
        normalY: Float,
        normalZ: Float,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        return super.createRect(
            x00,
            y00,
            z00,
            x10,
            y10,
            z10,
            x11,
            y11,
            z11,
            x01,
            y01,
            z01,
            normalX,
            normalY,
            normalZ,
            material,
            attributes
        )
    }

    override fun createRect(
        x00: Float,
        y00: Float,
        z00: Float,
        x10: Float,
        y10: Float,
        z10: Float,
        x11: Float,
        y11: Float,
        z11: Float,
        x01: Float,
        y01: Float,
        z01: Float,
        normalX: Float,
        normalY: Float,
        normalZ: Float,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        return super.createRect(
            x00,
            y00,
            z00,
            x10,
            y10,
            z10,
            x11,
            y11,
            z11,
            x01,
            y01,
            z01,
            normalX,
            normalY,
            normalZ,
            primitiveType,
            material,
            attributes
        )
    }

    override fun createCylinder(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCylinder(width, height, depth, divisions, material, attributes)
    }

    override fun createCylinder(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCylinder(width, height, depth, divisions, primitiveType, material, attributes)
    }

    override fun createCylinder(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        material: Material?,
        attributes: Long,
        angleFrom: Float,
        angleTo: Float
    ): Model {
        return Model()
        //return super.createCylinder(width, height, depth, divisions, material, attributes, angleFrom, angleTo)
    }

    override fun createCylinder(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long,
        angleFrom: Float,
        angleTo: Float
    ): Model {
        return Model()
        return super.createCylinder(
            width,
            height,
            depth,
            divisions,
            primitiveType,
            material,
            attributes,
            angleFrom,
            angleTo
        )
    }

    override fun createCone(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCone(width, height, depth, divisions, material, attributes)
    }

    override fun createCone(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCone(width, height, depth, divisions, primitiveType, material, attributes)
    }

    override fun createCone(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        material: Material?,
        attributes: Long,
        angleFrom: Float,
        angleTo: Float
    ): Model {
        return Model()
        //return super.createCone(width, height, depth, divisions, material, attributes, angleFrom, angleTo)
    }

    override fun createCone(
        width: Float,
        height: Float,
        depth: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long,
        angleFrom: Float,
        angleTo: Float
    ): Model {
        return Model()
        return super.createCone(
            width,
            height,
            depth,
            divisions,
            primitiveType,
            material,
            attributes,
            angleFrom,
            angleTo
        )
    }

    override fun createSphere(
        width: Float,
        height: Float,
        depth: Float,
        divisionsU: Int,
        divisionsV: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createSphere(width, height, depth, divisionsU, divisionsV, material, attributes)
    }

    override fun createSphere(
        width: Float,
        height: Float,
        depth: Float,
        divisionsU: Int,
        divisionsV: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createSphere(width, height, depth, divisionsU, divisionsV, primitiveType, material, attributes)
    }

    override fun createSphere(
        width: Float,
        height: Float,
        depth: Float,
        divisionsU: Int,
        divisionsV: Int,
        material: Material?,
        attributes: Long,
        angleUFrom: Float,
        angleUTo: Float,
        angleVFrom: Float,
        angleVTo: Float
    ): Model {
        return Model()
        return super.createSphere(
            width,
            height,
            depth,
            divisionsU,
            divisionsV,
            material,
            attributes,
            angleUFrom,
            angleUTo,
            angleVFrom,
            angleVTo
        )
    }

    override fun createSphere(
        width: Float,
        height: Float,
        depth: Float,
        divisionsU: Int,
        divisionsV: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long,
        angleUFrom: Float,
        angleUTo: Float,
        angleVFrom: Float,
        angleVTo: Float
    ): Model {
        return Model()
        return super.createSphere(
            width,
            height,
            depth,
            divisionsU,
            divisionsV,
            primitiveType,
            material,
            attributes,
            angleUFrom,
            angleUTo,
            angleVFrom,
            angleVTo
        )
    }

    override fun createCapsule(
        radius: Float,
        height: Float,
        divisions: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCapsule(radius, height, divisions, material, attributes)
    }

    override fun createCapsule(
        radius: Float,
        height: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createCapsule(radius, height, divisions, primitiveType, material, attributes)
    }

    override fun createXYZCoordinates(
        axisLength: Float,
        capLength: Float,
        stemThickness: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        return super.createXYZCoordinates(
            axisLength,
            capLength,
            stemThickness,
            divisions,
            primitiveType,
            material,
            attributes
        )
    }

    override fun createXYZCoordinates(axisLength: Float, material: Material?, attributes: Long): Model {
        return Model()
        //return super.createXYZCoordinates(axisLength, material, attributes)
    }

    override fun createArrow(
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        capLength: Float,
        stemThickness: Float,
        divisions: Int,
        primitiveType: Int,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        return super.createArrow(
            x1,
            y1,
            z1,
            x2,
            y2,
            z2,
            capLength,
            stemThickness,
            divisions,
            primitiveType,
            material,
            attributes
        )
    }

    override fun createArrow(from: Vector3?, to: Vector3?, material: Material?, attributes: Long): Model {
        return Model()
        //return super.createArrow(from, to, material, attributes)
    }

    override fun createLineGrid(
        xDivisions: Int,
        zDivisions: Int,
        xSize: Float,
        zSize: Float,
        material: Material?,
        attributes: Long
    ): Model {
        return Model()
        //return super.createLineGrid(xDivisions, zDivisions, xSize, zSize, material, attributes)
    }
}