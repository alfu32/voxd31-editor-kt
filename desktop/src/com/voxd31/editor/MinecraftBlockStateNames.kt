package com.voxd31.editor

/** Maps the protocol's 26.3 block state IDs back to their stable block names. */
internal object MinecraftBlockStateNames {
    private data class StateRange(val first: Int, val last: Int, val name: String)

    private val ranges: List<StateRange> by lazy {
        val stream = javaClass.getResourceAsStream("/minecraft/block-states-26.3.tsv")
            ?: return@lazy emptyList()
        stream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val fields = line.split('\t', limit = 3)
                if (fields.size != 3) null else StateRange(
                    fields[0].toInt(),
                    fields[1].toInt(),
                    fields[2]
                )
            }.toList()
        }
    }

    fun nameFor(stateId: Int): String {
        if (stateId == 0) return "minecraft:air"
        var low = 0
        var high = ranges.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val range = ranges[mid]
            when {
                stateId < range.first -> high = mid - 1
                stateId > range.last -> low = mid + 1
                else -> return range.name
            }
        }
        return MinecraftMaterials.default.id
    }
}
