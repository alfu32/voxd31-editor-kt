package com.voxd31.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.voxd31.gdxui.Cube
import com.voxd31.gdxui.EventListener
import com.voxd31.gdxui.ModelIntersection
import com.voxd31.gdxui.Vox3Event
import java.awt.event.InputEvent
import kotlin.math.floor


class InputEventDispatcher(
    val scene:SceneController,
    val camera2D: Camera,
    val camera3D: Camera,
    val guides:SceneController,
):InputProcessor {
    val listeners:MutableMap<String,MutableList<EventListener>> = mutableMapOf()
    var currentEvent = Vox3Event()

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
        currentEvent.keypressedMap[keycode]=keycode
        when(keycode) {
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> {
                currentEvent.ctrl=true
            }
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> {
                currentEvent.alt=true
            }
            Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> {
                currentEvent.shift=true
            }
            Input.Keys.F1 -> {

            }
            Input.Keys.F2 -> {

            }
            Input.Keys.F3 -> {

            }
            Input.Keys.F4 -> {

            }
            Input.Keys.F5 -> {

            }
            Input.Keys.F6 -> {

            }
            Input.Keys.F7 -> {

            }
            Input.Keys.F8 -> {

            }
            Input.Keys.F9 -> {

            }
            Input.Keys.F10 -> {

            }
            Input.Keys.F11 -> {

            }
            Input.Keys.F12 -> {

            }
        }
        dispatchEvents("keyDown")
        return true;
    }

    override fun keyUp(keycode: Int): Boolean {
        currentEvent.keyCode=keycode
        currentEvent.keyDown=0
        currentEvent.channel="keyUp"
        currentEvent.keypressedMap.remove(keycode)
        when(keycode) {
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> {
                currentEvent.ctrl=false
            }
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> {
                currentEvent.alt=false
            }
            Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> {
                currentEvent.shift=false
            }
            Input.Keys.F1 -> {

            }
            Input.Keys.F2 -> {

            }
            Input.Keys.F3 -> {

            }
            Input.Keys.F4 -> {

            }
            Input.Keys.F5 -> {

            }
            Input.Keys.F6 -> {

            }
            Input.Keys.F7 -> {

            }
            Input.Keys.F8 -> {

            }
            Input.Keys.F9 -> {

            }
            Input.Keys.F10 -> {

            }
            Input.Keys.F11 -> {

            }
            Input.Keys.F12 -> {

            }
        }
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
        currentEvent.keypressedMap[button]=button
        dispatchEvents("touchDown")
        return true;
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        currentEvent.channel="touchUp"
        currentEvent.keypressedMap.remove(button)
        dispatchEvents("touchUp")
        return true;
    }

    override fun touchCancelled(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        update3dVectorsFromScreenPoint(x, y)
        currentEvent.pointer=pointer
        currentEvent.button=button
        currentEvent.channel="touchCancelled"
        currentEvent.keypressedMap.remove(button)
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
        val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        camera2D.unproject(touchPos); // Transforms the touch/mouse position to world coordinates
        val X=touchPos.x
        val Y=touchPos.y

        currentEvent.screen = Vector2(X, Y)

        // Implement the conversion from screen coordinates to world coordinates
        val ray = camera3D.getPickRay(
            x.toFloat(), y.toFloat(),
            x/camera3D.viewportWidth, y/camera3D.viewportHeight,
            camera3D.viewportWidth,camera3D.viewportHeight,
        )
        var points= mutableListOf<ModelIntersection>()
        var modelIntersect = scene.sceneIntersectCubesRay(ray)
        if(modelIntersect.hit){
            points.add(modelIntersect.copy())
        }
        modelIntersect = guides.sceneIntersectCubesRay(ray)
        if(modelIntersect.hit){
            modelIntersect.type="guide"
            points.add(modelIntersect.copy())
        }
        val intersection = Vector3()
        Intersector.intersectRayPlane(ray, Plane(Vector3.Y, 0f), intersection)
        points.add(
            ModelIntersection(
                hit = true,
                point = intersection,
                normal = Vector3(0f,1f,0f),
                target = Cube(modelBuilder = ModelBuilder(),position = Vector3(intersection.x,intersection.y,intersection.z), color = Color.CYAN),
                type = "ground",
            )
        )
        val mi=points.minBy { mi0 -> mi0.point.dst2(ray.origin) }
        currentEvent.modelPoint = mi.point.cpy()
        val p = mi.target.position
        currentEvent.modelVoxel = Vector3(floor(p.x),floor(p.y),floor(p.z))
        currentEvent.normal = mi.normal
        currentEvent.target = mi.target
        currentEvent.modelNextPoint = modelIntersect.point.cpy().add(mi.normal)
        currentEvent.modelNextVoxel = currentEvent.modelVoxel!!.cpy().add(mi.normal)
        if(mi.type == "ground" || mi.type == "guide" ) {
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