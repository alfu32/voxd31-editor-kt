package com.voxd31.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.xovd3i.editor.Voxd31Editor
import kotlin.math.cos
import kotlin.math.sin

private fun measureText(layout:GlyphLayout,font:BitmapFont,text:String):Vector2{
    layout.setText(font, text)
    return Vector2(layout.width, layout.height)
}
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
    // Central rectangle
    // shapeRenderer.rect(x + radius, y + radius, width - 2 * radius, height - 2 * radius)

    // Four side lines
    // bottom
    shapeRenderer.line(x + radius, y, x+width - radius, y)
    // top
    shapeRenderer.line(x + radius, y + height, x+width - radius, y + height)
    // left
    shapeRenderer.line(x, y + radius, x, y+height - radius)
    // right
    shapeRenderer.line(x + width, y + radius, x + width, y+height - radius)

    // Four corner circles
    shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f)
    shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f)
    shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f)
    shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f)

    shapeRenderer.color = srcolor
}
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
    abstract fun drawText(spriteBatch:SpriteBatch)

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
open class UiElementsCollection(
    override var position:Vector2=Vector2(0f, 0f),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var hover: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var border: Color=Color.GRAY,
    open var elements: MutableList<UiElement> = mutableListOf()
):UiElement(position, size, background, hover, color, border,){
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
    override fun drawText(spriteBatch:SpriteBatch){
        for (e in elements) {
            val cl = spriteBatch.color
            spriteBatch.color = color
            e.drawText(spriteBatch)
            spriteBatch.color = cl
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

class UiElementLabel(
    override var position:Vector2=Vector2(0f, 0f),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var text:String="",
    var font: String="default",
    var radius:Float=0f,
) : UiElement(position, size, background, Color.WHITE, color, Color.GRAY, text) {
    override fun draw(shapeRenderer2d:ShapeRenderer){
        drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,background)
    }
    override fun drawLines(shapeRenderer2d:ShapeRenderer){
    }
    override fun drawText(spriteBatch:SpriteBatch){
        val layout = GlyphLayout()
        val font = Voxd31Editor.fonts[font]!!
        layout.setText(font, text)
        val sz=size.cpy().sub(layout.width,layout.height).scl(0.5f,0.5f)
        val cl = spriteBatch.color
        spriteBatch.color = color
        font.draw(spriteBatch,text,position.x+sz.x, position.y+sz.y+layout.height)
        spriteBatch.color = cl
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
    var font: String="default",
    var radius:Float=0f,
    override var clicked:(target:UiElement,event:Event)->Unit={ t,e -> }
):UiElement(position, size, background, hover, color, border, text, clicked){
    override fun draw(shapeRenderer2d:ShapeRenderer){
        drawRoundedRectangle(shapeRenderer2d,position.x, position.y,size.x,size.y,radius,background)
        val cl = shapeRenderer2d.color
        shapeRenderer2d.color=border
        val r = radius // Math.max(radius,5f)
        shapeRenderer2d.circle(position.x+r-1,position.y+r,r)
        shapeRenderer2d.rect(position.x-1,position.y+r,2*r,size.y-2*r)
        shapeRenderer2d.circle(position.x+r-1,position.y+size.y-r,r)
        shapeRenderer2d.color=background
        shapeRenderer2d.rect(position.x+r,position.y,r,size.y)
        shapeRenderer2d.color=cl
        //shapeRenderer2d.rect(position.x, position.y,size.x,size.y,hover,hover,hover,useColor)
    }
    override fun drawLines(shapeRenderer2d:ShapeRenderer){
        val useColor = if(isHovered){ hover } else { border }
        drawRoundedRectangleLines(shapeRenderer2d,position.x,position.y,size.x,size.y,radius,useColor)
        drawRoundedRectangleLines(shapeRenderer2d,position.x-1,position.y-1,size.x+2,size.y+2,radius,useColor)
        //shapeRenderer2d.rect(position.x,position.y,size.x,size.y,color,color,color,color)
        //shapeRenderer2d.rect(position.x-1,position.y-1,size.x+2,size.y+2,color,color,color,color)
    }
    override fun drawText(spriteBatch:SpriteBatch){
        val layout = GlyphLayout()
        val font = Voxd31Editor.fonts[font]!!
        layout.setText(font, text)
        val sz=size.cpy().sub(layout.width,layout.height).scl(0.5f,0.5f)
        val cl = spriteBatch.color
        spriteBatch.color = if(isHovered){ background } else { color }
        font.draw(spriteBatch,text,position.x+sz.x, position.y+sz.y+layout.height)
        spriteBatch.color = cl
    }
}
class UiElementOptgroup<T>(
    var options: List<T> = listOf(),
    override var position:Vector2=Vector2(0f, 0f),
    override var size: Vector2=Vector2(20f,20f),
    override var background: Color =Color.WHITE,
    override var hover: Color =Color.WHITE,
    override var color: Color=Color.BLACK,
    override var border: Color=Color.GRAY,
    var font: String="default",
    var label:String="",
    var changed:(target:UiElement,ev:Event,oldValue:T,newValue:T)->Unit={ t,e,o,n -> }
):UiElementsCollection(position, size, background, hover, color, border,mutableListOf()) {
    val layout = GlyphLayout()
    var selectedIndex = 0
    init{
        val fnt = Voxd31Editor.fonts[font]!!
        layout.setText(fnt, "__${label}_:_${options.joinToString ("__")}")
        size.set(layout.width.coerceAtLeast(size.x),
            layout.height.coerceAtLeast(size.y))
        elements = mutableListOf()
        layout.setText(fnt, "_${label}_:_")
        val prev=Rectangle(position.x,position.y,layout.width,layout.height)
        elements.add(UiElementLabel(
            position=Vector2(position.x,position.y),
            size=Vector2(layout.width,size.y),
            background=background,
            color=color,
            font=font,
            text=label,
        ))
        options.forEachIndexed(){ i,opt ->
            layout.setText(fnt, "__$opt")
            val current=Rectangle(prev.x+prev.width,position.y,layout.width,layout.height)
            elements.add(
                UiElementButton(
                    position=Vector2(prev.x+prev.width,position.y),
                    size=Vector2(layout.width,size.y),
                    background=background,
                    hover=hover,
                    color=color,
                    border=border,
                    font=font,
                    text=opt.toString()
                ){ target,ev ->

                    if (target.isClicked && ev.channel == "touchDown") {
                        changed(target,ev,options[selectedIndex],options[i])
                        selectedIndex = i
                    }
                    target.background = if (selectedIndex == i) hover else background
                    target.color = if (selectedIndex == i) border else color
                }
            )
            prev.set(current)
        }
    }
}

