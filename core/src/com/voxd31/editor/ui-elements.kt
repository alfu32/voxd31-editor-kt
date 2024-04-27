package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import kotlin.math.cos
import kotlin.math.sin


class UIStyle()
abstract class UiElement(
    open var position:Vector2=Vector2(0f, 0f),
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
        // p.y= Gdx.graphics.height - position.y - 40
        // p.x = p.x + 20f
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
    override var position:Vector2=Vector2(0f, 0f),
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
    override var position:Vector2=Vector2(0f, 0f),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var hover: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var border: Color=Color.GRAY,
    override var text:String="",
    /**
     * possible values : box, pill
     */
    var radius:Float=0f,
    override var clicked:(target:UiElement,event:Event)->Unit={ t,e -> }
):UiElement(position, size, background, hover, color, border, text, clicked){
    private fun drawRoundedRectangle(
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color
    ) {
        val srcolor=shapeRenderer.color
        shapeRenderer.color = color
        // Central rectangle
        shapeRenderer.rect(x + radius, y + radius, width - 2 * radius, height - 2 * radius)

        // Four side rectangles
        shapeRenderer.rect(x + radius, y, width - 2 * radius, radius)
        shapeRenderer.rect(x + radius, y + height - radius, width - 2 * radius, radius)
        shapeRenderer.rect(x, y + radius, radius, height - 2 * radius)
        shapeRenderer.rect(x + width - radius, y + radius, radius, height - 2 * radius)

        // Four corner circles
        shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f)
        shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f)
        shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f)
        shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f)
        shapeRenderer.color = srcolor
    }
    private fun drawRoundedRectangleLines(
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color
    ) {
        val srcolor=shapeRenderer.color
        shapeRenderer.color = color

        // Total points for the corners
        val cornerPoints = if(radius<1.0f) 1 else (Math.PI*radius/2).toInt()// More points for a smoother corner


        // Create an array to hold points for the polyline
        val points = FloatArray((cornerPoints + 1) * 8) // +1 for each segment in corner and 4 corners


        // Helper function to add points to the array
        var index = 0

        // Upper left corner
        for (i in 0..cornerPoints) {
            val angle = Math.PI / 2 * (i / cornerPoints.toDouble())
            points[index++] = x + radius - (radius * cos(angle)).toFloat()
            points[index++] = y + height - radius + (radius * sin(angle)).toFloat()
        }

        // Upper right corner
        for (i in 0..cornerPoints) {
            val angle = Math.PI / 2 * (i / cornerPoints.toDouble())
            points[index++] = x + width - radius + (radius * cos(angle)).toFloat()
            points[index++] = y + height - radius + (radius * sin(angle)).toFloat()
        }

        // Lower right corner
        for (i in 0..cornerPoints) {
            val angle = Math.PI / 2 * (i / cornerPoints.toDouble())
            points[index++] = x + width - radius + (radius * cos(angle)).toFloat()
            points[index++] = y + radius - (radius * sin(angle)).toFloat()
        }

        // Lower left corner
        for (i in 0..cornerPoints) {
            val angle = Math.PI / 2 * (i / cornerPoints.toDouble())
            points[index++] = x + radius - (radius * cos(angle)).toFloat()
            points[index++] = y + radius - (radius * sin(angle)).toFloat()
        }


        // Draw the polyline
        shapeRenderer.polyline(points)

        shapeRenderer.color = srcolor
    }
    override fun draw(shapeRenderer2d:ShapeRenderer){
        if(isHovered){
            drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,hover)
            //shapeRenderer2d.rect(position.x, position.y,size.x,size.y,hover,hover,hover,hover)
        } else {
            drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,background)
            //shapeRenderer2d.rect(position.x,position.y,size.x,size.y,background,background,background,background)
        }
    }
    override fun drawLines(shapeRenderer2d:ShapeRenderer){
        drawRoundedRectangleLines(shapeRenderer2d,position.x,position.y,size.x,size.y,radius,color)
        drawRoundedRectangleLines(shapeRenderer2d,position.x-1,position.y-1,size.x+2,size.y+2,radius,color)
        //shapeRenderer2d.rect(position.x,position.y,size.x,size.y,color,color,color,color)
        //shapeRenderer2d.rect(position.x-1,position.y-1,size.x+2,size.y+2,color,color,color,color)
    }
    override fun drawText(spriteBatch:SpriteBatch, font: BitmapFont){
        val layout = GlyphLayout()
        layout.setText(font, text)
        val sz=size.cpy().sub(layout.width,layout.height).scl(0.5f,0.5f)
        font.draw(spriteBatch,text,position.x+sz.x, position.y+sz.y+layout.height)
    }
}