package com.voxd31.editor.render

data class ShadowSettings(
    var shadowBias: Float,
    var shadowNormalBias: Float,
    var pcfMode: Int,
    var dither: Boolean,
    var useShadows: Boolean
)
