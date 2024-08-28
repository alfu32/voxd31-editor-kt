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
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    var label:String="",
    var changed:(target: UiElement, ev:Vox3Event, oldValue:T, newValue:T)->Unit={ t, e, o, n -> }
): UiElementsCollection(position, size, normalStyle, hoverStyle,focusStyle,mutableListOf()) {
    val layout = GlyphLayout()
    var selectedIndex = 0
    fun init(): UiElementOptgroup<T> {
        layout.setText(currentStyle().font.bitmapFont(), "__${label}_:_${options.joinToString ("__")}")
        size.set(layout.width.coerceAtLeast(size.x+4),
            layout.height.coerceAtLeast(size.y+4))
        elements = mutableListOf()
        layout.setText(currentStyle().font.bitmapFont(), "_${label}_:_")
        var prev= Rectangle(position.x,position.y,layout.width,layout.height)
        var maxOptSize=Vector2(0f,size.y)
        options.forEachIndexed(){ i,opt ->
            layout.setText(currentStyle().font.bitmapFont(), "__$opt")
            maxOptSize= Vector2(kotlin.math.max(maxOptSize.x,layout.width),kotlin.math.max(maxOptSize.y,layout.height))
            val current= Rectangle(prev.x+prev.width+25,position.y,layout.width+35,layout.height)
            prev.set(current)
        }
        prev= Rectangle(position.x,position.y,layout.width+55,layout.height)
        elements.add(
            UiElementLabel(
                position= Vector2(position.x,position.y),
                size= Vector2(maxOptSize.x,maxOptSize.y),
                normalStyle=normalStyle,
                hoverStyle=hoverStyle,
                text=label,
            )
        )
        options.forEachIndexed(){ i,opt ->
            layout.setText(currentStyle().font.bitmapFont(), "__$opt")
            val current= Rectangle(prev.x+prev.width+15,position.y,layout.width+15,maxOptSize.y)
            elements.add(
                UiElementCanvasButton(
                    position= Vector2(prev.x+prev.width+15,position.y),
                    size= Vector2(layout.width+15,maxOptSize.y),
                    normalStyle=normalStyle,
                    hoverStyle=hoverStyle,
                    focusStyle=focusStyle,
                    text=opt.toString(),
                    drawFigure = { self,ctx,shapeRenderer2d ->
                        val sz=size.y/2+2
                        val x=current.x
                        val y=current.y+sz-1
                        when(ctx) {
                            UiDrawingContext.FILL -> {
                                shapeRenderer2d.color=self.currentStyle().border
                                shapeRenderer2d.circle(x, y, sz)
                                shapeRenderer2d.color=self.currentStyle().background
                                shapeRenderer2d.circle(x, y, sz-2f)
                                shapeRenderer2d.color=self.currentStyle().border
                                shapeRenderer2d.circle(x, y, sz-4f)
                            }
                            UiDrawingContext.LINE -> {}
                            UiDrawingContext.TEXT -> {}
                            UiDrawingContext.BACKGROUND -> {}
                            UiDrawingContext.BORDER -> {}
                        }
                    }
                ){ target:UiElement,ev:Vox3Event ->
                    if (target.isClicked && ev.channel == "touchDown") {
                        println("touchDown")
                        setElementFocus(selectedIndex,false)
                        changed(target, ev, options[selectedIndex], options[i])
                        selectedIndex = i
                        setElementFocus(selectedIndex,true)
                    }
                }
            )
            prev.set(current)
        }
        setElementFocus(selectedIndex,true)
        return this
    }
    fun setElementFocus(index:Int,value:Boolean) {
        elements[1+index].hasFocus=value
        (elements[0] as UiElementLabel).text=options[index].toString()
    }
}