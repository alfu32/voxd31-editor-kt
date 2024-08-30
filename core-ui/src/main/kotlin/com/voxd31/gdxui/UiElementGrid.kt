package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import kotlin.math.max

class UiGridCell(
    val text:String,
    val background:Color = UiStyle.defaultNormal().background,
    val foreground:Color = UiStyle.defaultNormal().color,
){
    override fun toString(): String {
        return "{text:$text,background:$background,foreground:$foreground}"
    }
}
class UiElementGrid(
    var elementSize:Vector2= Vector2(20f,20f),
    var data:List<List<UiGridCell>> = listOf(listOf()),
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    var changed:(target: UiElementGrid, ev:Vox3Event, c0: UiGridCell, c1: UiGridCell)->Unit={ t, e, c0, c1 -> }
): UiElementsCollection(position, size, normalStyle, hoverStyle,focusStyle,mutableListOf()) {
    var selectedCell = UiGridCell("")
    var selectedOrd=0
    var calculatedSize=size.cpy()
    override fun init(): UiElement {
        elements.clear()
        val self= this

        val csz=elementSize
        var ord=0

        data.forEachIndexed{ j,row ->
            row.forEachIndexed{ i,cell ->
                val ord0=ord
                println("[$i,$j] ord:$ord sz:${elements.size} ord0:$ord0")
                elements.add(
                    UiElementButton(
                        text=cell.text,
                        position = Vector2(
                            (csz.x+5)*i+position.x,
                            (csz.y+5)*j+position.y,
                        ),
                        size=csz.cpy(),
                        radius = 3f,
                    ){ target:UiElement,ev:Vox3Event ->
                        if (target.isClicked && ev.channel == "touchDown") {
                            println("touchDown")
                            setElementFocus(selectedOrd,false)
                            val cell0 = selectedCell
                            selectedCell = cell
                            selectedOrd = ord0
                            changed(self, ev,cell0,cell)
                            setElementFocus(selectedOrd,true)
                        }
                    }
                )
                ord+=1
            }
        }
        setElementFocus(selectedOrd,true)
        return this
    }
    fun setElementFocus(ord:Int,value:Boolean) {
        elements[ord].hasFocus=value
    }
    override fun getRectangle():Rectangle{
        val calculatedSize=size.cpy()
        val csz=elementSize
        data.forEachIndexed { j, row ->
            calculatedSize.y += csz.y + 5
            row.forEachIndexed { i, cell ->
                calculatedSize.x = max((csz.x + 5) * i, calculatedSize.x)
            }
        }
        return Rectangle(
            this.position.x,this.position.y,
            this.calculatedSize.x,calculatedSize.y,
        )
    }
}