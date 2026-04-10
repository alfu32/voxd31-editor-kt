package com.voxd31.editor.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider

class VoxcraftShaderProvider(
    private val shadowSettingsProvider: () -> ShadowSettings,
    private val shadowLightProvider: () -> DirectionalShadowLight
) : DefaultShaderProvider(createConfig()) {

    override fun createShader(renderable: Renderable): Shader {
        return VoxcraftDefaultShader(renderable, config, shadowSettingsProvider, shadowLightProvider)
    }

    companion object {
        private fun createConfig(): DefaultShader.Config {
            val vertex = Gdx.files.internal("shaders/voxcraft-default.vertex.glsl").readString("UTF-8")
            val fragment = Gdx.files.internal("shaders/voxcraft-default.fragment.glsl").readString("UTF-8")
            return DefaultShader.Config(vertex, fragment)
        }
    }
}
