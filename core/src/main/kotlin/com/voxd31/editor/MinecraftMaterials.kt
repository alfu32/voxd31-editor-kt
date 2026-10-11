package com.voxd31.editor

import com.badlogic.gdx.graphics.Color

data class MinecraftMaterial(val id: String) {
    val label: String = id.substringAfter(':').replace('_', ' ')
        .split(' ').joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
}

object MinecraftMaterials {
    private val names = """
        stone granite polished_granite diorite polished_diorite andesite polished_andesite deepslate cobbled_deepslate
        polished_deepslate calcite tuff dripstone_block moss_block dirt coarse_dirt rooted_dirt grass_block podzol mycelium
        clay gravel sand red_sand sandstone cut_sandstone smooth_sandstone red_sandstone cut_red_sandstone smooth_red_sandstone
        cobblestone mossy_cobblestone stone_bricks cracked_stone_bricks mossy_stone_bricks bricks mud packed_mud mud_bricks
        bedrock cobbled_deepslate_bricks deepslate_bricks quartz_block smooth_quartz quartz_bricks quartz_pillar
        obsidian crying_obsidian netherrack nether_bricks red_nether_bricks blackstone polished_blackstone basalt
        polished_basalt end_stone end_stone_bricks purpur_block prismarine prismarine_bricks dark_prismarine
        oak_planks spruce_planks birch_planks jungle_planks acacia_planks dark_oak_planks mangrove_planks cherry_planks
        bamboo_planks crimson_planks warped_planks oak_log spruce_log birch_log jungle_log acacia_log dark_oak_log
        mangrove_log cherry_log bamboo_block crimson_stem warped_stem oak_leaves spruce_leaves birch_leaves jungle_leaves
        acacia_leaves dark_oak_leaves mangrove_leaves cherry_leaves glass tinted_glass white_stained_glass orange_stained_glass
        magenta_stained_glass light_blue_stained_glass yellow_stained_glass lime_stained_glass pink_stained_glass gray_stained_glass
        light_gray_stained_glass cyan_stained_glass purple_stained_glass blue_stained_glass brown_stained_glass green_stained_glass
        red_stained_glass black_stained_glass white_concrete orange_concrete magenta_concrete light_blue_concrete yellow_concrete
        lime_concrete pink_concrete gray_concrete light_gray_concrete cyan_concrete purple_concrete blue_concrete brown_concrete
        green_concrete red_concrete black_concrete white_wool orange_wool magenta_wool light_blue_wool yellow_wool lime_wool
        pink_wool gray_wool light_gray_wool cyan_wool purple_wool blue_wool brown_wool green_wool red_wool black_wool
        copper_block exposed_copper weathered_copper oxidized_copper cut_copper iron_block gold_block diamond_block emerald_block
        lapis_block redstone_block coal_block netherite_block amethyst_block honeycomb_block hay_block bone_block sponge
        wet_sponge sea_lantern glowstone shroomlight sea_pickle bookshelf chiseled_bookshelf crafting_table furnace chest barrel
        white_terracotta orange_terracotta magenta_terracotta light_blue_terracotta yellow_terracotta lime_terracotta pink_terracotta
        gray_terracotta light_gray_terracotta cyan_terracotta purple_terracotta blue_terracotta brown_terracotta green_terracotta
        red_terracotta black_terracotta terracotta snow_block ice packed_ice blue_ice powder_snow
    """.trimIndent().split(Regex("\\s+")).distinct()

    val all: List<MinecraftMaterial> = names.map { MinecraftMaterial("minecraft:$it") }
    val default: MinecraftMaterial = MinecraftMaterial("minecraft:stone")

    fun colorFor(id: String?): Color {
        val name = id?.substringAfter(':') ?: return Color.WHITE.cpy()
        val color = when {
            name == "air" -> "ffffff"
            name.contains("grass") || name.contains("leaves") || name.contains("moss") ||
                name.contains("fern") || name.contains("azalea") -> "70a84b"
            name.contains("water") || name.contains("prismarine") -> "4d9db4"
            name.contains("sand") -> "d6c18a"
            name.contains("dirt") || name.contains("mud") || name.contains("podzol") ||
                name.contains("mycelium") || name.contains("rooted_dirt") -> "805738"
            name.contains("wood") || name.contains("log") || name.contains("planks") ||
                name.contains("stem") || name.contains("hyphae") -> "9b704a"
            name.contains("glass") -> "a9d8e3"
            name.contains("wool") || name.contains("concrete") || name.contains("terracotta") ||
                name.contains("candle") || name.contains("shulker") -> dyeColor(name) ?: "c6c0b8"
            name.contains("gold") || name.contains("hay") -> "e0bd4c"
            name.contains("diamond") || name.contains("emerald") -> "42c99a"
            name.contains("redstone") || name.contains("nether") || name.contains("netherrack") ||
                name.contains("cinnabar") -> "a84e42"
            name.contains("amethyst") || name.contains("purpur") -> "a77bc1"
            name.contains("ice") || name.contains("snow") -> "a9d8e3"
            name.contains("copper") -> "b8734f"
            name.contains("ore") || name.contains("stone") || name.contains("deepslate") ||
                name.contains("basalt") || name.contains("tuff") || name.contains("calcite") ||
                name.contains("bedrock") -> "858585"
            else -> "a0a0a0"
        }
        return Color.valueOf(color)
    }

    private fun dyeColor(name: String): String? {
        val colors = mapOf(
            "white" to "e8e5dc", "orange" to "e58b35", "magenta" to "b856b8",
            "light_blue" to "6ca6d9", "yellow" to "e5d34b", "lime" to "8dc342",
            "pink" to "e58da7", "gray" to "686868", "light_gray" to "a9a9a1",
            "cyan" to "398b8b", "purple" to "8155a6", "blue" to "465bb6",
            "brown" to "795338", "green" to "557d35", "red" to "b9413b",
            "black" to "303033"
        )
        return colors.entries.firstOrNull { (color, _) -> name.startsWith("${color}_") }?.value
    }
}
