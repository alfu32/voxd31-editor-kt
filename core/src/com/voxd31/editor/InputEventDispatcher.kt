package com.voxd31.editor

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.math.floor

class Event(
    var keyCode: Int? = null,
    var keyDown: Int? = null,
    var screen:Vector2? = null,
    var scroll:Vector2? = null,
    var modelPoint: Vector3? = null,
    var modelVoxel: Vector3? = null,
    var modelNextPoint: Vector3? = null,
    var modelNextVoxel: Vector3? = null,
    var target:Cube? = null,
    var normal:Vector3? = null,
    var pointer:Int? = null,
    var button:Int? = null,
    var channel:String = "none"
) {
    override fun toString(): String {
        return "keyDown:$keyDown , screen:$screen , model:$modelPoint ,modelNext:$modelNextPoint , pointer:$pointer , button:$button , target:${target?.getId()}"
    }
}

typealias EventListener = (e:Event)->Unit
class InputEventDispatcher(
    val scene:SceneController,
    val camera: Camera,
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
        listeners[channel]!!.forEach{listener ->
            listener(currentEvent)
        }
    }
    override fun keyDown(keycode: Int): Boolean {
        currentEvent.keyCode=keycode
        currentEvent.keyDown=keycode
        currentEvent.channel="keyDown"
        dispatchEvents("keyDown")
        return true;
    }

    override fun keyUp(keycode: Int): Boolean {
        currentEvent.keyCode=keycode
        currentEvent.keyDown=0
        currentEvent.channel="keyUp"
        dispatchEvents("keyUp")
        return true;
    }

    override fun keyTyped(character: Char): Boolean {
        currentEvent.channel="keyTyped"
        dispatchEvents("keyTyped")
        return true;
    }

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        currentEvent.channel="touchDown"
        dispatchEvents("touchDown")
        return true;
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        currentEvent.channel="touchUp"
        dispatchEvents("touchUp")
        return true;
    }

    override fun touchCancelled(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        currentEvent.channel="touchCancelled"
        dispatchEvents("touchCancelled")
        return false
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.channel="touchDragged"
        dispatchEvents("touchDragged")
        return true;
    }

    override fun mouseMoved(x: Int, y: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.channel="mouseMoved"
        dispatchEvents("mouseMoved")
        return true;
    }
    private fun Vector3Round(v:Vector3): Vector3 {
        return Vector3(
            floor(v.x),
            floor(v.y),
            floor(v.z),
        )
    }

    private fun update3dVectorsFromScreenPoint(x: Int, y: Int) {
        val X=x.toFloat()
        val Y=y.toFloat()
        currentEvent.screen = Vector2(X, Y)

        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera.getPickRay(
            X, Y,
            X/camera.viewportWidth, Y/camera.viewportHeight,
            camera.viewportWidth,camera.viewportHeight,
        )
        val modelIntersect = scene.sceneIntersectRay(ray)
        currentEvent.modelPoint = modelIntersect.point.cpy()
        val p = modelIntersect.target.position
        currentEvent.modelVoxel = Vector3(floor(p.x),floor(p.y),floor(p.z))
        currentEvent.normal = modelIntersect.normal
        currentEvent.target = modelIntersect.target
        currentEvent.modelNextPoint = modelIntersect.point.cpy().add(modelIntersect.normal)
        currentEvent.modelNextVoxel = currentEvent.modelVoxel!!.cpy().add(modelIntersect.normal)
        if(modelIntersect.type == "ground") {
            currentEvent.modelNextPoint = currentEvent.modelPoint
            currentEvent.modelNextVoxel = currentEvent.modelVoxel
        }

        // println("3d points ${modelIntersect.type} : $currentEvent")
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        currentEvent.scroll = Vector2(amountX,amountY)
        return true;
    }
}