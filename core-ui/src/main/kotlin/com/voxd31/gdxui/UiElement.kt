package com.voxd31.gdxui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

abstract class UiElement(
    open var position: Vector2 = Vector2(0f, 0f),
    open var size: Vector2 = Vector2(20f,20f),
    open var normalStyle:UiStyle=UiStyle.defaultNormal(),
    open var hoverStyle:UiStyle=UiStyle.defaultHover(),
    open var focusStyle:UiStyle=UiStyle.defaultNormal(),
    open var text:String="",
    open var clicked:(target: UiElement, event: Vox3Event)->Unit={ t, e -> }
){
    var isHovered = false
    var isClicked = false
    var isPressed = false
    var hasFocus = false
    abstract fun draw(shapeRenderer2d: ShapeRenderer)
    abstract fun drawLines(shapeRenderer2d: ShapeRenderer)
    abstract fun drawText(spriteBatch: SpriteBatch)

    open fun currentStyle():UiStyle = if(isHovered){
        hoverStyle
    } else if (isPressed || hasFocus) {
        focusStyle
    }else {
        normalStyle
    }
    open fun dispatch(e:Vox3Event){
        isClicked=false
        isHovered=false
        isPressed = false
        val p = position.cpy()
        // p.y= Gdx.graphics.height - position.y - 40
        // p.x = p.x + 20f
        if (
            e.screen != null &&
            e.screen!!.x > p.x && e.screen!!.x < p.x + size.x &&
            e.screen!!.y > p.y && e.screen!!.y < p.y + size.y
        ){
            isHovered = true
            isPressed = e.button != null && (e.button == Input.Buttons.LEFT || e.button == Input.Buttons.MIDDLE || e.button == Input.Buttons.RIGHT)
            isClicked = e.button != null && (e.button == Input.Buttons.LEFT || e.button == Input.Buttons.MIDDLE || e.button == Input.Buttons.RIGHT)
        } else {
            isHovered=false
            isClicked=false
            isPressed = false
        }
        // if(isClicked) {
        clicked(this, e)
        // }
    }

    override fun toString(): String {
        return """
            {
                position: ${position},
                size: ${size},
                style: ${normalStyle},
                hover: ${hoverStyle},
                text: ${text}, 
            }
        """.trimIndent()
    }

    fun getBoundingBox(): BoundingBox {
        val p= Vector3(position.x,1f,position.y)
        val s= Vector3(size.x,1f,size.y)
        return BoundingBox(p,p.cpy().add(s))
    }
}