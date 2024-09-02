package com.voxd31.gdxui

import com.badlogic.gdx.math.Rectangle

// Extension function to get the union of two rectangles (a new rectangle containing both)
fun Rectangle.add(other: Rectangle): Rectangle {
    val minX = minOf(this.x, other.x)
    val minY = minOf(this.y, other.y)
    val maxX = maxOf(this.x + this.width, other.x + other.width)
    val maxY = maxOf(this.y + this.height, other.y + other.height)

    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}

// Extension function to offset a rectangle by a scalar value (geometrically)
fun Rectangle.offset(scalar: Float): Rectangle {
    return Rectangle(
        this.x - scalar,
        this.y - scalar,
        this.width + 2 * scalar,
        this.height + 2 * scalar
    )
}