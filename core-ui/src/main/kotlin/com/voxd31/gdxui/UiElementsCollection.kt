package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.collision.BoundingBox

typealias LayoutRule = (current:UiElement,index:Int,all:List<UiElement>) -> Vector2
val LayoutRuleAbsolute:LayoutRule={ c,i,a ->
    c.position
}
val LayoutRuleFlow:LayoutRule={ c,i,a ->
    c.position
}
open class UiElementsCollection(
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var background: Color = Color.WHITE,
    override var hover: Color = Color.WHITE,
    override var color: Color = Color.BLACK,
    override var border: Color = Color.GRAY,
    open var elements: MutableList<UiElement> = mutableListOf(),
    /**
     * default layout rule is absolute positioning based on each element position vector
     */
    open var layout_rule: LayoutRule =LayoutRuleAbsolute
): UiElement(position, size, background, hover, color, border,){
    override fun draw(shapeRenderer2d: ShapeRenderer){
        for (e in elements) {
            e.draw(shapeRenderer2d)
        }
    }
    override fun drawLines(shapeRenderer2d: ShapeRenderer){
        for (e in elements) {
            e.drawLines(shapeRenderer2d)
        }
    }
    override fun drawText(spriteBatch: SpriteBatch, fonts:Map<String, BitmapFont>){
        for (e in elements) {
            val cl = spriteBatch.color
            spriteBatch.color = color
            e.drawText(spriteBatch,fonts)
            spriteBatch.color = cl
        }
    }

    override fun dispatch(e:Vox3Event){
        this.isClicked=false
        this.isHovered=false
        for (el in elements) {
            el.dispatch(e)
            if(el.isHovered){
                this.isHovered=true
                if(el.isClicked){
                    this.isClicked=true
                }
                break
            }
            if(el.isClicked){
                this.isClicked=true
                break
            }
        }
    }
    fun add(element: UiElement): UiElementsCollection {
        elements.add(element)
        return this
    }
    fun addAll(els: Collection<UiElement>): UiElementsCollection {
        elements.addAll(els)
        return this
    }

    override fun toString(): String {
        return elements.joinToString("\n") { it.toString() }
    }
}