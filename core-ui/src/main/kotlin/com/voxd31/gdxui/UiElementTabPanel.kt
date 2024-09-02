package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
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
    var rect:Rectangle = Rectangle(position.x,position.y,size.x,size.y)
    override fun init(): UiElement {
        rect = Rectangle(position.x,position.y,size.x,size.y)
        elements = mutableListOf()
        val bb0 = BoundingBox().set(arrayOf(
            Vector3(Vector2(0f,0f),0f)//,Vector3(position.cpy().add(size),0f)
        ))
        println("bb0=$bb0")
        var prev= Rectangle(position.x,position.y,bb0.width,bb0.height)
        val self= this
        tabs.forEachIndexed { i,pair ->
            val bb =pair.first.getRectangle()
            val current= Rectangle(prev.x+prev.width+5,position.y,bb.width,bb.height)

            // rect=rect
            //     .add(Rectangle(current.x,current.y,current.width,current.height))
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
                    }.init()
                    .apply {
                        rect=rect.add(this.getRectangle())
                    },
                    pair.second.apply {
                        this.position.x=self.position.x + 10f
                        this.position.y=self.position.y - 10f - pair.first.getRectangle().height
                        this.isVisible=false
                    }.init()
                    .apply {
                        rect=rect.add(this.getRectangle())
                    }
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

    override fun draw(shapeRenderer2d: ShapeRenderer) {
        val stl = currentStyle()
        val cl = shapeRenderer2d.color
        shapeRenderer2d.color=stl.color
        shapeRenderer2d.rect(position.x,position.y-rect.height,rect.width,rect.height)
        shapeRenderer2d.color=cl
        super.draw(shapeRenderer2d)
    }

    override fun drawLines(shapeRenderer2d: ShapeRenderer) {
        val stl = currentStyle()
        val cl = shapeRenderer2d.color
        shapeRenderer2d.color=stl.border
        shapeRenderer2d.rect(position.x,position.y-rect.height,rect.width,rect.height)
        shapeRenderer2d.color=cl
        super.drawLines(shapeRenderer2d)
    }
}