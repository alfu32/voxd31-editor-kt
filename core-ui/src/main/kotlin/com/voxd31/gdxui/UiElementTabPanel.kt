package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import kotlin.math.max

class UiElementTabPanel(
    var tabs: MutableList<Pair<UiElementButton,UiElement>> = mutableListOf(),
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    var changed:(target: UiElementTabPanel, ev:Vox3Event,i0: Int,i1: Int)->Unit={ t, e,i0,i1 -> }
): UiElementsCollection(position, size, normalStyle, hoverStyle,focusStyle,mutableListOf()) {
    var selectedIndex = 0
    override fun init(): UiElement {
        elements = mutableListOf()
        val bb0 = BoundingBox().set(arrayOf(
            Vector3(Vector2(0f,0f),0f)//,Vector3(position.cpy().add(size),0f)
        ))
        println("bb0=$bb0")
        var prev= Rectangle(position.x,position.y,this.size.x,this.size.y)
        var maxTabsSize=Vector2(size.x,size.y)
        var maxPanelSize=Vector2(size.x,size.y)
        tabs.forEachIndexed { i,pair ->
            val bb =pair.first.getRectangle()
            val bb2 =pair.second.getRectangle()
            maxTabsSize= Vector2(max(maxTabsSize.x,bb.width),max(maxTabsSize.y,bb.height))
            maxPanelSize= Vector2(max(maxPanelSize.x,bb2.width),max(maxPanelSize.y,bb2.height))
        }
        prev= Rectangle(position.x,position.y,bb0.width,bb0.height)
        val self= this
        tabs.forEachIndexed { i,pair ->
            val bb =pair.first.getRectangle()
            println("bb[$i]=$bb")
            val current= Rectangle(prev.x+prev.width+5,position.y,bb.width,bb.height)
            println("current[$i]=$current")
            elements.addAll(
                listOf(
                    pair.first.apply{
                        this.clicked = { target:UiElement,ev:Vox3Event ->
                            if (target.isClicked && ev.channel == "touchDown") {
                                println("touchDown")
                                setElementFocus(selectedIndex,false)
                                val j = selectedIndex
                                selectedIndex = i
                                changed(self, ev,j,i)
                                setElementFocus(selectedIndex,true)
                            }
                        }
                        this.position.x=current.x
                        this.position.y=current.y
                    }.init(),
                    pair.second.apply {
                        val rect=this.getRectangle()
                        println(rect)
                        this.position.x=self.position.x
                        this.position.y=self.position.y-rect.height
                        this.isVisible=false
                    }.init()
                )
            )
            prev.set(current)
        }
        setElementFocus(selectedIndex,true)
        return this
    }
    fun setElementFocus(index:Int,value:Boolean) {
        tabs[index].first.hasFocus=value
        tabs[index].second.isVisible=value
    }
}