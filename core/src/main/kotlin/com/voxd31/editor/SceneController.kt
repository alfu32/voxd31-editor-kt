package com.voxd31.editor

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.voxd31.gdxui.Cube
import com.voxd31.gdxui.ModelIntersection
import kotlin.math.floor



class SceneController(val modelBuilder: ModelBuilder) {
    private data class ChunkCoord(val x: Int, val y: Int, val z: Int)
    private data class GreedyRect(val u: Int, val v: Int, val width: Int, val height: Int, val colorKey: Int)
    private data class MergedFace(val corners: Array<Vector3>, val normal: Vector3)

    companion object {
        private const val CHUNK_SIZE = 16
        private val TOP_NORMAL = Vector3(0f, 1f, 0f)
        private val BOTTOM_NORMAL = Vector3(0f, -1f, 0f)
        private val LEFT_NORMAL = Vector3(-1f, 0f, 0f)
        private val RIGHT_NORMAL = Vector3(1f, 0f, 0f)
        private val BACK_NORMAL = Vector3(0f, 0f, -1f)
        private val FRONT_NORMAL = Vector3(0f, 0f, 1f)
    }

    var cubes: HashMap<String, Cube> = hashMapOf()
    var cubesInt: HashMap<String,Cube> = hashMapOf()
    var currentColor: Color = Color.RED
    private var renderCacheDirty = true
    private val cachedModels = mutableListOf<Model>()
    private val cachedRenderInstances = mutableListOf<ModelInstance>()
    private val cachedShadowInstances = mutableListOf<ModelInstance>()

    /**
     * possible values :
     *  - addWithoutReplace
     *  - addOrReplace
     *  - replaceCube
     * default value is addOrReplace
     */
    var addMode = "addOrReplace"
    fun sceneIntersectCubesRay(ray: Ray): ModelIntersection {
        val intersections = cubes.map { (id, cube) -> cube.intersectsRay(ray) }.filterNotNull().filter{ it.hit }
        if (!intersections.isEmpty()) {
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(ray.origin) }!!
        } else {
            return ModelIntersection(
                hit = false,
                point = Vector3(),
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(), color = Color.CYAN),
                type = "origin",
            )
        }
    }

    fun sceneIntersectGuidesRay(ray: Ray): ModelIntersection {
        val intersections = cubes.map { (id, cube) -> cube.intersectsGuidesRay(ray) }.filterNotNull().filter{ it.hit }
        if (!intersections.isEmpty()) {
            return intersections.minByOrNull { mi -> mi.point.cpy().dst2(ray.origin) }!!
        } else {
            return ModelIntersection(
                hit = false,
                point = Vector3(),
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(), color = Color.CYAN),
                type = "origin",
            )
        }
    }
    fun addCube(position: Vector3,color:Color? = null) {
        when(addMode){
            "addWithoutReplace" ->{
                add(position,color)
            }
            "addOrReplace" ->{
                addOrReplaceCube(position,color)
            }
            "replaceCube" ->{
                replaceCube(position,color)
            }
        }
    }
    fun add(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        if(cubes[cube.getId()] == null ) {
            cubes[cube.getId()] = cube
            cubesInt[cube.getIntId()] = cube
            invalidateRenderCache()
        }/// println("cubes : ${cubes.size}")
    }
    fun addOrReplaceCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        cubes[cube.getId()]=cube
        cubesInt[cube.getIntId()]=cube
        invalidateRenderCache()
        /// println("cubes : ${cubes.size}")
    }
    fun replaceCube(position: Vector3,color:Color? = null) {
        val cube = createCubeAt(position,color)
        if(cubes[cube.getId()] != null ) {
            cubes[cube.getId()] = cube
            cubesInt[cube.getIntId()]=cube
            invalidateRenderCache()
        }
        /// println("cubes : ${cubes.size}")
    }
    fun removeCube(c: Cube) {
        cubes.remove(c.getId())
        cubesInt.remove(c.getIntId())
        invalidateRenderCache()
    }
    fun createCubeAt(p:Vector3,color:Color? = null): Cube {
        // Implementation to create a cube ModelInstance at the specified coordinates
        // Placeholder implementation
        return Cube(modelBuilder, position = Vector3(floor(p.x),floor(p.y),floor(p.z)), color ?: currentColor)
    }
    fun clear() {
        cubes = hashMapOf()
        cubesInt = hashMapOf()
        invalidateRenderCache()
    }

    fun dispose() {
        disposeRenderCache()
    }

    fun removeCube(v: Vector3) {
        val c=createCubeAt(v)
        removeCube(c)
    }

    fun cubeAt(p: Vector3): Cube? {
        val c=createCubeAt(p)
        return cubes[c.getId()]
    }

    fun renderInstances(): List<ModelInstance> {
        ensureRenderCache()
        return cachedRenderInstances
    }

    fun shadowInstances(): List<ModelInstance> {
        ensureRenderCache()
        return cachedShadowInstances
    }

    internal fun visibleRenderChunkCount(): Int {
        return collectChunkCoords().size
    }

    internal fun visibleRenderFaceCount(): Int {
        return collectChunkCoords().sumOf { chunk ->
            collectChunkFaces(chunk).values.sumOf { faces -> faces.size }
        }
    }

    internal fun hasConsistentFaceWinding(): Boolean {
        val edgeA = Vector3()
        val edgeB = Vector3()
        val computedNormal = Vector3()
        return collectChunkCoords().all { chunk ->
            collectChunkFaces(chunk).values.flatten().all { face ->
                edgeA.set(face.corners[1]).sub(face.corners[0])
                edgeB.set(face.corners[2]).sub(face.corners[0])
                computedNormal.set(edgeA).crs(edgeB)
                computedNormal.dot(face.normal) > 0f
            }
        }
    }

    private fun invalidateRenderCache() {
        renderCacheDirty = true
    }

    private fun ensureRenderCache() {
        if (!renderCacheDirty) {
            return
        }
        rebuildRenderCache()
    }

    private fun rebuildRenderCache() {
        disposeRenderCache()
        renderCacheDirty = false
        if (cubes.isEmpty()) {
            return
        }

        collectChunkCoords().sortedWith(compareBy<ChunkCoord>({ it.x }, { it.y }, { it.z })).forEach { chunk ->
            val renderFaces = collectChunkFaces(chunk)
            buildChunkModel(chunk, renderFaces, opaqueOnly = false)?.let { model ->
                cachedModels += model
                cachedRenderInstances += ModelInstance(model)
            }
            buildChunkModel(chunk, renderFaces, opaqueOnly = true)?.let { model ->
                cachedModels += model
                cachedShadowInstances += ModelInstance(model)
            }
        }
    }

    private fun addFace(part: MeshPartBuilder, corners: Array<Vector3>, normal: Vector3) {
        part.rect(corners[0], corners[1], corners[2], corners[3], normal)
    }

    private fun collectChunkCoords(): Set<ChunkCoord> {
        val chunks = linkedSetOf<ChunkCoord>()
        cubes.values.forEach { cube ->
            val x = floor(cube.position.x).toInt()
            val y = floor(cube.position.y).toInt()
            val z = floor(cube.position.z).toInt()
            chunks += chunkCoordFor(x, y, z)
        }
        return chunks
    }

    private fun chunkCoordFor(x: Int, y: Int, z: Int): ChunkCoord {
        return ChunkCoord(
            Math.floorDiv(x, CHUNK_SIZE),
            Math.floorDiv(y, CHUNK_SIZE),
            Math.floorDiv(z, CHUNK_SIZE)
        )
    }

    private fun collectChunkFaces(chunk: ChunkCoord): Map<Int, MutableList<MergedFace>> {
        val facesByColor = linkedMapOf<Int, MutableList<MergedFace>>()
        val startX = chunk.x * CHUNK_SIZE
        val startY = chunk.y * CHUNK_SIZE
        val startZ = chunk.z * CHUNK_SIZE

        appendTopFaces(facesByColor, startX, startY, startZ)
        appendBottomFaces(facesByColor, startX, startY, startZ)
        appendFrontFaces(facesByColor, startX, startY, startZ)
        appendBackFaces(facesByColor, startX, startY, startZ)
        appendRightFaces(facesByColor, startX, startY, startZ)
        appendLeftFaces(facesByColor, startX, startY, startZ)

        return facesByColor
    }

    private fun appendTopFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (y in startY until startY + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lx, lz ->
                val x = startX + lx
                val z = startZ + lz
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x, y + 1, z)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val x0 = startX + rect.u
                val x1 = x0 + rect.width - 1
                val z0 = startZ + rect.v
                val z1 = z0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x0 - Cube.DL, y + Cube.DR, z0 - Cube.DL),
                        Vector3(x0 - Cube.DL, y + Cube.DR, z1 + Cube.DR),
                        Vector3(x1 + Cube.DR, y + Cube.DR, z1 + Cube.DR),
                        Vector3(x1 + Cube.DR, y + Cube.DR, z0 - Cube.DL)
                    ),
                    TOP_NORMAL
                )
            }
        }
    }

    private fun appendBottomFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (y in startY until startY + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lx, lz ->
                val x = startX + lx
                val z = startZ + lz
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x, y - 1, z)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val x0 = startX + rect.u
                val x1 = x0 + rect.width - 1
                val z0 = startZ + rect.v
                val z1 = z0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x0 - Cube.DL, y - Cube.DL, z1 + Cube.DR),
                        Vector3(x0 - Cube.DL, y - Cube.DL, z0 - Cube.DL),
                        Vector3(x1 + Cube.DR, y - Cube.DL, z0 - Cube.DL),
                        Vector3(x1 + Cube.DR, y - Cube.DL, z1 + Cube.DR)
                    ),
                    BOTTOM_NORMAL
                )
            }
        }
    }

    private fun appendFrontFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (z in startZ until startZ + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lx, ly ->
                val x = startX + lx
                val y = startY + ly
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x, y, z + 1)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val x0 = startX + rect.u
                val x1 = x0 + rect.width - 1
                val y0 = startY + rect.v
                val y1 = y0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x0 - Cube.DL, y0 - Cube.DL, z + Cube.DR),
                        Vector3(x1 + Cube.DR, y0 - Cube.DL, z + Cube.DR),
                        Vector3(x1 + Cube.DR, y1 + Cube.DR, z + Cube.DR),
                        Vector3(x0 - Cube.DL, y1 + Cube.DR, z + Cube.DR)
                    ),
                    FRONT_NORMAL
                )
            }
        }
    }

    private fun appendBackFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (z in startZ until startZ + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lx, ly ->
                val x = startX + lx
                val y = startY + ly
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x, y, z - 1)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val x0 = startX + rect.u
                val x1 = x0 + rect.width - 1
                val y0 = startY + rect.v
                val y1 = y0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x1 + Cube.DR, y0 - Cube.DL, z - Cube.DL),
                        Vector3(x0 - Cube.DL, y0 - Cube.DL, z - Cube.DL),
                        Vector3(x0 - Cube.DL, y1 + Cube.DR, z - Cube.DL),
                        Vector3(x1 + Cube.DR, y1 + Cube.DR, z - Cube.DL)
                    ),
                    BACK_NORMAL
                )
            }
        }
    }

    private fun appendRightFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (x in startX until startX + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lz, ly ->
                val z = startZ + lz
                val y = startY + ly
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x + 1, y, z)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val z0 = startZ + rect.u
                val z1 = z0 + rect.width - 1
                val y0 = startY + rect.v
                val y1 = y0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x + Cube.DR, y0 - Cube.DL, z1 + Cube.DR),
                        Vector3(x + Cube.DR, y0 - Cube.DL, z0 - Cube.DL),
                        Vector3(x + Cube.DR, y1 + Cube.DR, z0 - Cube.DL),
                        Vector3(x + Cube.DR, y1 + Cube.DR, z1 + Cube.DR)
                    ),
                    RIGHT_NORMAL
                )
            }
        }
    }

    private fun appendLeftFaces(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        startX: Int,
        startY: Int,
        startZ: Int
    ) {
        for (x in startX until startX + CHUNK_SIZE) {
            collectRectangles(CHUNK_SIZE, CHUNK_SIZE) { lz, ly ->
                val z = startZ + lz
                val y = startY + ly
                val cube = cubeAtInt(x, y, z) ?: return@collectRectangles null
                if (hasCubeAt(x - 1, y, z)) null else Color.rgba8888(cube.color)
            }.forEach { rect ->
                val z0 = startZ + rect.u
                val z1 = z0 + rect.width - 1
                val y0 = startY + rect.v
                val y1 = y0 + rect.height - 1
                addMergedFace(
                    facesByColor,
                    rect.colorKey,
                    arrayOf(
                        Vector3(x - Cube.DL, y0 - Cube.DL, z0 - Cube.DL),
                        Vector3(x - Cube.DL, y0 - Cube.DL, z1 + Cube.DR),
                        Vector3(x - Cube.DL, y1 + Cube.DR, z1 + Cube.DR),
                        Vector3(x - Cube.DL, y1 + Cube.DR, z0 - Cube.DL)
                    ),
                    LEFT_NORMAL
                )
            }
        }
    }

    private fun collectRectangles(
        width: Int,
        height: Int,
        colorAt: (u: Int, v: Int) -> Int?
    ): List<GreedyRect> {
        val mask = arrayOfNulls<Int>(width * height)
        val consumed = BooleanArray(width * height)
        for (v in 0 until height) {
            for (u in 0 until width) {
                mask[v * width + u] = colorAt(u, v)
            }
        }

        val rects = mutableListOf<GreedyRect>()
        for (v in 0 until height) {
            for (u in 0 until width) {
                val index = v * width + u
                val colorKey = mask[index] ?: continue
                if (consumed[index]) {
                    continue
                }

                var rectWidth = 1
                while (u + rectWidth < width) {
                    val nextIndex = v * width + u + rectWidth
                    if (consumed[nextIndex] || mask[nextIndex] != colorKey) {
                        break
                    }
                    rectWidth += 1
                }

                var rectHeight = 1
                heightLoop@ while (v + rectHeight < height) {
                    for (du in 0 until rectWidth) {
                        val nextIndex = (v + rectHeight) * width + u + du
                        if (consumed[nextIndex] || mask[nextIndex] != colorKey) {
                            break@heightLoop
                        }
                    }
                    rectHeight += 1
                }

                for (dv in 0 until rectHeight) {
                    for (du in 0 until rectWidth) {
                        consumed[(v + dv) * width + u + du] = true
                    }
                }
                rects += GreedyRect(u, v, rectWidth, rectHeight, colorKey)
            }
        }
        return rects
    }

    private fun addMergedFace(
        facesByColor: MutableMap<Int, MutableList<MergedFace>>,
        colorKey: Int,
        corners: Array<Vector3>,
        normal: Vector3
    ) {
        facesByColor.getOrPut(colorKey) { mutableListOf() }.add(MergedFace(corners, normal))
    }

    private fun buildChunkModel(
        chunk: ChunkCoord,
        facesByColor: Map<Int, MutableList<MergedFace>>,
        opaqueOnly: Boolean
    ): Model? {
        val filtered = facesByColor
            .filterKeys { colorKey -> !opaqueOnly || isOpaqueColor(colorKey) }
            .filterValues { it.isNotEmpty() }
        if (filtered.isEmpty()) {
            return null
        }

        val builder = ModelBuilder()
        builder.begin()
        filtered.toSortedMap().forEach { (colorKey, faces) ->
            val color = Color()
            Color.rgba8888ToColor(color, colorKey)
            val material = Material(ColorAttribute.createDiffuse(color))
            if (color.a < 0.99f) {
                material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, color.a))
            }
            val part = builder.part(
                "chunk-${chunk.x}-${chunk.y}-${chunk.z}-$colorKey",
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong(),
                material
            )
            faces.forEach { face ->
                addFace(part, face.corners, face.normal)
            }
        }
        return builder.end()
    }

    private fun isOpaqueColor(colorKey: Int): Boolean {
        val color = Color()
        Color.rgba8888ToColor(color, colorKey)
        return color.a >= 0.99f
    }

    private fun hasCubeAt(x: Int, y: Int, z: Int): Boolean {
        return cubesInt.containsKey(intId(x, y, z))
    }

    private fun cubeAtInt(x: Int, y: Int, z: Int): Cube? {
        return cubesInt[intId(x, y, z)]
    }

    private fun intId(x: Int, y: Int, z: Int): String {
        return "{$x,$y,$z}"
    }

    private fun disposeRenderCache() {
        cachedModels.forEach { it.dispose() }
        cachedModels.clear()
        cachedRenderInstances.clear()
        cachedShadowInstances.clear()
    }
}
