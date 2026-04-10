package com.voxd31.editor.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext

class VoxcraftDefaultShader(
    renderable: Renderable,
    config: Config,
    private val shadowSettingsProvider: () -> ShadowSettings,
    private val shadowLightProvider: () -> DirectionalShadowLight
) : DefaultShader(renderable, config) {

    private val uShadowBias = register("u_shadowBias")
    private val uShadowNormalBias = register("u_shadowNormalBias")
    private val uShadowMapSize = register("u_shadowMapSize")
    private val uShadowPcfMode = register("u_shadowPcfMode")
    private val uShadowDither = register("u_shadowDither")
    private val uShadowUseShadows = register("u_shadowUseShadows")
    private val uShadowLightDir = register("u_shadowLightDir")

    override fun begin(camera: Camera, context: RenderContext) {
        super.begin(camera, context)
        val settings = shadowSettingsProvider()
        if (has(uShadowBias)) {
            set(uShadowBias, settings.shadowBias)
        }
        if (has(uShadowNormalBias)) {
            set(uShadowNormalBias, settings.shadowNormalBias)
        }
        if (has(uShadowPcfMode)) {
            set(uShadowPcfMode, settings.pcfMode.toFloat())
        }
        if (has(uShadowDither)) {
            set(uShadowDither, if (settings.dither) 1f else 0f)
        }
        if (has(uShadowUseShadows)) {
            set(uShadowUseShadows, if (settings.useShadows) 1f else 0f)
        }

        val light = shadowLightProvider()
        if (has(uShadowMapSize)) {
            set(uShadowMapSize, light.frameBuffer.width.toFloat())
        }
        if (has(uShadowLightDir)) {
            set(uShadowLightDir, light.direction)
        }
    }
}
