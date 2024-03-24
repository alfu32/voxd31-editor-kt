package com.voxd31.editor

import com.badlogic.gdx.InputProcessor

class CompositeInputProcessor : InputProcessor {
    var processors:MutableList<InputProcessor> = mutableListOf()

    fun addInputProcessor(ip: InputProcessor) {
        processors.add(ip)
    }
    fun removeInputProcessor(ip: InputProcessor) {
        processors.remove(ip)
    }
    override fun keyDown(keycode: Int): Boolean {
        processors.forEach{it.keyDown(keycode)}
        return false;
    }

    override fun keyUp(keycode: Int): Boolean {
        processors.forEach{it.keyUp(keycode)}
        return false;
    }

    override fun keyTyped(character: Char): Boolean {
        processors.forEach{it.keyTyped(character)}
        return false;
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        processors.forEach{it.touchDown(x, y, pointer, button)}
        return false;
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        processors.forEach{it.touchUp(x, y, pointer, button)}
        return false;
    }

    override fun touchCancelled(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        processors.forEach{it.touchCancelled(x, y, pointer, button)}
        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        processors.forEach{it.touchDragged(x, y, pointer)}
        return false;
    }

    override fun mouseMoved(x: Int, y: Int): Boolean {
        processors.forEach{it.mouseMoved(x, y)}
        return false;
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        processors.forEach{it.scrolled(amountX, amountY)}
        return false;
    }
}