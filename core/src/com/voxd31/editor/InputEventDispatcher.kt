package com.voxd31.editor

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class Event(
    var keyCode: Int? = null,
    var keyDown: Int? = null,
    var screen:Vector2? = null,
    var scroll:Vector2? = null,
    var model: Vector3? = null,
    var target:Cube? = null,
    var normal:Vector3? = null,
    var pointer:Int? = null,
    var button:Int? = null,
)

typealias EventListener = (e:Event)->Unit
class InputEventDispatcher(
    val scene:SceneController,

):InputProcessor {
    val listeners:MutableMap<String,MutableList<EventListener>> = mutableMapOf()
    var currentEvent = Event()

    fun on(channel:String, listener:EventListener) {
        if(! listeners.containsKey((channel))) {
            listeners[channel]= mutableListOf()
        }
        listeners[channel]!!.add(listener)
    }
    fun dispatchEvents(channel: String) {
        if(! listeners.containsKey((channel))) {
            listeners[channel]= mutableListOf()
        }
        listeners[channel]!!.forEach{
            it(currentEvent)
        }
    }
    override fun keyDown(keycode: Int): Boolean {
        currentEvent.keyCode=keycode
        currentEvent.keyDown=keycode
        dispatchEvents("keyDown")
        return false;
    }

    override fun keyUp(keycode: Int): Boolean {
        currentEvent.keyCode=keycode
        currentEvent.keyDown=0
        dispatchEvents("keyUp")
        return false;
    }

    override fun keyTyped(character: Char): Boolean {
        dispatchEvents("keyTyped")
        return false;
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        currentEvent.screen= Vector2(x.toFloat(),y.toFloat())
        currentEvent.model = scene.screenToModelPoint(x,y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        dispatchEvents("touchDown")
        return false;
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        currentEvent.screen= Vector2(x.toFloat(),y.toFloat())
        currentEvent.model = scene.screenToModelPoint(x,y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        dispatchEvents("touchUp")
        return false;
    }

    override fun touchCancelled(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        currentEvent.screen= Vector2(x.toFloat(),y.toFloat())
        currentEvent.model = scene.screenToModelPoint(x,y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        dispatchEvents("touchCancelled")
        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        currentEvent.screen= Vector2(x.toFloat(),y.toFloat())
        currentEvent.model = scene.screenToModelPoint(x,y)
        currentEvent.pointer=pointer
        dispatchEvents("touchDragged")
        return false;
    }

    override fun mouseMoved(x: Int, y: Int): Boolean {
        currentEvent.screen= Vector2(x.toFloat(),y.toFloat())
        currentEvent.model = scene.screenToModelPoint(x,y)
        dispatchEvents("mouseMoved")
        return false;
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        currentEvent.scroll = Vector2(amountX,amountY)
        return false;
    }
}