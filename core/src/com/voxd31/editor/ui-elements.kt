package com.voxd31.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
class UIStyle()
abstract class UiElement(
    open var position:Vector2=Vector2(0f,0f,),
    open var size: Vector2=Vector2(20f,20f),
    open var background: Color =Color.WHITE,
    open var hover: Color =Color.WHITE,
    open var color: Color=Color.BLACK,
    open var border: Color=Color.GRAY,
    open var text:String="",
    open var clicked:(target:UiElement,event:Event)->Unit={ t,e -> }
){
    var isHovered = false
    var isClicked = false
    abstract fun draw(shapeRenderer2d:ShapeRenderer)
    abstract fun drawLines(shapeRenderer2d:ShapeRenderer)
    abstract fun drawText(spriteBatch:SpriteBatch,font: BitmapFont)

    open fun dispatch(e:Event){
        isClicked=false
        isHovered=false
        val p = position.cpy()
        p.y= Gdx.graphics.height - position.y - 40
        if (
            e.screen != null &&
            e.screen!!.x > p.x && e.screen!!.x < p.x + size.x &&
            e.screen!!.y > p.y && e.screen!!.y < p.y + size.y
        ){
            isHovered = true
            if(
                e.button != null && e.button == Input.Buttons.LEFT
            ) {
                isClicked=true
            } else {
                isClicked=false
            }
        } else {
            isHovered=false
        }
        clicked(this,e)
    }

    override fun toString(): String {
        return """
            {
                position: ${position},
                size: ${size},
                background: ${background},
                hover: ${hover},
                color: ${color},
                border: ${border},
                text: ${text}, 
            }
        """.trimIndent()
    }
}
class UiElementsCollection(
    override var position:Vector2=Vector2(0f,0f,),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var hover: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var border: Color=Color.GRAY,
    var elements: MutableList<UiElement> = mutableListOf()
):UiElement(position, size, background, hover, color, border){
    override fun draw(shapeRenderer2d:ShapeRenderer){
        for (e in elements) {
            e.draw(shapeRenderer2d)
        }
    }
    override fun drawLines(shapeRenderer2d:ShapeRenderer){
        for (e in elements) {
            e.drawLines(shapeRenderer2d)
        }
    }
    override fun drawText(spriteBatch:SpriteBatch, font: BitmapFont){
        for (e in elements) {
            e.drawText(spriteBatch,font)
        }
    }

    override fun dispatch(e:Event){
        isClicked=false
        isHovered=false
        for (el in elements) {
            el.dispatch(e)
            if(el.isHovered){
                isHovered=true
            }
            if(el.isClicked){
                isClicked=true
            }
        }
    }
    fun add(element: UiElement):UiElementsCollection {
        elements.add(element)
        return this
    }
    fun addAll(els: Collection<UiElement>):UiElementsCollection {
        elements.addAll(els)
        return this
    }

    override fun toString(): String {
        return elements.joinToString("\n") { it.toString() }
    }
}
class UiElementButton(
    override var position:Vector2=Vector2(0f,0f,),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var hover: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var border: Color=Color.GRAY,
    override var text:String="",
    override var clicked:(target:UiElement,event:Event)->Unit={ t,e -> }
):UiElement(position, size, background, hover, color, border, text, clicked){
    override fun draw(shapeRenderer2d:ShapeRenderer){
        if(isHovered){
            shapeRenderer2d.rect(position.x, position.y,size.x,size.y,hover,hover,hover,hover)
        } else {
            shapeRenderer2d.rect(position.x,position.y,size.x,size.y,background,background,background,background)
        }
    }
    override fun drawLines(shapeRenderer2d:ShapeRenderer){
        shapeRenderer2d.rect(position.x,position.y,size.x,size.y,color,color,color,color)
    }
    override fun drawText(spriteBatch:SpriteBatch, font: BitmapFont){
        font.draw(spriteBatch,text,position.x+1, position.y+15)
    }
}