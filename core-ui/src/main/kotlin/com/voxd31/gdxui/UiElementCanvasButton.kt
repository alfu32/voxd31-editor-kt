package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2

class UiElementCanvasButton(
    override var position: Vector2 = Vector2(0f, 0f),
    override var size: Vector2 = Vector2(20f,20f),
    override var normalStyle:UiStyle=UiStyle.defaultNormal(),
    override var hoverStyle:UiStyle=UiStyle.defaultHover(),
    override var focusStyle:UiStyle=UiStyle.defaultFocus(),
    override var text:String="",
    override var radius:Float=0f,
    var drawFigure:(UiElement,UiDrawingContext,ShapeRenderer)->Unit = {self,dc,sr -> },
    override var clicked:(target: UiElement, event:Vox3Event)->Unit={ t, e -> }
): UiElementButton(position, size, normalStyle, hoverStyle,focusStyle, text, radius, clicked){
    override fun draw(shapeRenderer2d: ShapeRenderer){
        super.draw(shapeRenderer2d)
        val cl = shapeRenderer2d.color
        drawFigure(this,UiDrawingContext.FILL,shapeRenderer2d)
        shapeRenderer2d.color = cl
    }
    override fun drawLines(shapeRenderer2d: ShapeRenderer){
        super.drawLines(shapeRenderer2d)
        val cl = shapeRenderer2d.color
        drawFigure(this,UiDrawingContext.LINE,shapeRenderer2d)
        shapeRenderer2d.color = cl
    }
}