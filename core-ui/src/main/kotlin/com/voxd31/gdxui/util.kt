package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2


private fun measureText(layout: GlyphLayout, font: BitmapFont, text:String): Vector2 {
    layout.setText(font, text)
    return Vector2(layout.width, layout.height)
}
fun drawRoundedRectangle(
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
fun drawRoundedRectangleLines(
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
    try{shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f)}catch(_:Throwable){}
    try{shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f)}catch(_:Throwable){}
    try{shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f)}catch(_:Throwable){}
    try{shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f)}catch(_:Throwable){}

    shapeRenderer.color = srcolor
}

fun <T : Number> range(start: T, end: T, step: T = 1 as T): List<T> {
    val result = mutableListOf<T>()

    var current = start.toDouble()
    val endValue = end.toDouble()
    val stepValue = step.toDouble()

    if (stepValue == 0.0) throw IllegalArgumentException("Step value must be non-zero")

    while ((stepValue > 0 && current <= endValue) || (stepValue < 0 && current >= endValue)) {
        result.add(current as T)
        current += stepValue
    }

    return result
}