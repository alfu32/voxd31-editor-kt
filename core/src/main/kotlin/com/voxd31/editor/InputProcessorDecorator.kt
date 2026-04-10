package com.voxd31.editor

import com.badlogic.gdx.InputProcessor

interface InputProcessorDecorator {
    fun wrap(inputProcessor: InputProcessor): InputProcessor
}

object PassthroughInputProcessorDecorator : InputProcessorDecorator {
    override fun wrap(inputProcessor: InputProcessor): InputProcessor = inputProcessor
}
