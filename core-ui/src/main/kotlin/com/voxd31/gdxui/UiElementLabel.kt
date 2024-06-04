package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class UiElementLabel(
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    override var text:String="",
    var radius:Float=0f,
) : UiElement(position, size, normalStyle, hoverStyle,focusStyle, text) {
    override fun draw(shapeRenderer2d: ShapeRenderer){
        drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,currentStyle().background)
    }
    override fun drawLines(shapeRenderer2d: ShapeRenderer){
    }
    override fun drawText(spriteBatch: SpriteBatch, fonts:Map<String, BitmapFont>){
        val layout = GlyphLayout()
        val font = fonts[currentStyle().font_id]!!
        layout.setText(font, text)
        val sz=size.cpy().sub(layout.width,layout.height).scl(0.5f,0.5f)
        val cl = spriteBatch.color
        spriteBatch.color = currentStyle().color
        font.draw(spriteBatch,text,position.x+sz.x, position.y+sz.y+layout.height)
        spriteBatch.color = cl
    }
}