package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

open class UiElementButton(
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    override var text:String="",
    open var radius:Float=0f,
    override var clicked:(target: UiElement, event:Vox3Event)->Unit={ t, e -> }
): UiElement(position, size, normalStyle, hoverStyle,focusStyle, text, clicked){
    override fun draw(shapeRenderer2d: ShapeRenderer){
        drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,currentStyle().background)
        val cl = shapeRenderer2d.color
        shapeRenderer2d.color=currentStyle().border
        val r = radius // Math.max(radius,5f)
        shapeRenderer2d.circle(position.x+r-1,position.y+r,r)
        shapeRenderer2d.rect(position.x-1,position.y+r,2*r,size.y-2*r)
        shapeRenderer2d.circle(position.x+r-1,position.y+size.y-r,r)
        shapeRenderer2d.color=normalStyle.color
        shapeRenderer2d.rect(position.x+r,position.y,r,size.y)
        shapeRenderer2d.color=cl
        //shapeRenderer2d.rect(position.x, position.y,size.x,size.y,hover,hover,hover,useColor)
    }
    override fun drawLines(shapeRenderer2d: ShapeRenderer){
        drawRoundedRectangleLines(shapeRenderer2d,position.x,position.y,size.x,size.y,radius,currentStyle().border)
        drawRoundedRectangleLines(shapeRenderer2d,position.x-1,position.y-1,size.x+2,size.y+2,radius,currentStyle().border)
        //shapeRenderer2d.rect(position.x,position.y,size.x,size.y,color,color,color,color)
        //shapeRenderer2d.rect(position.x-1,position.y-1,size.x+2,size.y+2,color,color,color,color)
    }
    override fun drawText(spriteBatch: SpriteBatch){
        val layout = GlyphLayout()
        val font = currentStyle().font.bitmapFont()
        layout.setText(font, text)
        val sz=size.cpy().sub(layout.width,layout.height).scl(0.5f,0.5f)
        val cl = spriteBatch.color
        spriteBatch.color = currentStyle().color
        font.draw(spriteBatch,text,position.x+sz.x, position.y+sz.y+layout.height)
        spriteBatch.color = cl
    }
}