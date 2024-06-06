package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

class UiElementOptgroup<T>(
    var options: List<T> = listOf(),
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultNormal(),
    var label:String="",
    var changed:(target: UiElement, ev:Vox3Event, oldValue:T, newValue:T)->Unit={ t, e, o, n -> }
): UiElementsCollection(position, size, normalStyle, hoverStyle,focusStyle,mutableListOf()) {
    val layout = GlyphLayout()
    var selectedIndex = 0
    fun init(): UiElementOptgroup<T> {
        layout.setText(currentStyle().font.bitmapFont(), "__${label}_:_${options.joinToString ("__")}")
        size.set(layout.width.coerceAtLeast(size.x),
            layout.height.coerceAtLeast(size.y))
        elements = mutableListOf()
        layout.setText(currentStyle().font.bitmapFont(), "_${label}_:_")
        val prev= Rectangle(position.x,position.y,layout.width,layout.height)
        elements.add(
            UiElementLabel(
                position= Vector2(position.x,position.y),
                size= Vector2(layout.width,size.y),
                normalStyle=normalStyle,
                hoverStyle=hoverStyle,
                text=label,
            )
        )
        options.forEachIndexed(){ i,opt ->
            layout.setText(currentStyle().font.bitmapFont(), "__$opt")
            val current= Rectangle(prev.x+prev.width,position.y,layout.width,layout.height)
            elements.add(
                UiElementButton(
                    position= Vector2(prev.x+prev.width,position.y),
                    size= Vector2(layout.width,size.y),
                    normalStyle=normalStyle,
                    hoverStyle=hoverStyle,
                    focusStyle=focusStyle,
                    text=opt.toString()
                ){ target,ev ->

                    if (target.isClicked && ev.channel == "touchDown") {
                        changed(target,ev,options[selectedIndex],options[i])
                        selectedIndex = i
                    }
                }
            )
            prev.set(current)
        }
        return this
    }
}