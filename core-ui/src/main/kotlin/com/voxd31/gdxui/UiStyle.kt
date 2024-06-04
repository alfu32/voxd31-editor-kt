package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color


class UiStyle(
    var color: Color = Color.BLACK,
    var background: Color = Color.WHITE,
    var border: Color= Color.BLUE,
    var font_id:String="noto-sans-regular 16px black",
) {
    companion object{
        fun defaultNormal() = UiStyle(
            color=Color.BLACK,
            background = Color(0x090809ff),
            border = Color(0x090809ff),
            font_id = "noto-sans-regular 16px light",
        )
        fun defaultHover() = UiStyle(
            color=Color.LIGHT_GRAY,
            background = Color(0x0f0e0fff),
            border = Color.CYAN,
            font_id = "noto-sans-regular 16px highlight",
        )
        fun defaultFocus() = UiStyle(
            color=Color.BLACK,
            background = Color(0x090809ff),
            border = Color.GOLD,
            font_id = "noto-sans-regular 16px light",
        )
    }
    fun cpy() = UiStyle(color,background,border)
}
class UiStyleSheet(
    var normal: UiStyle = UiStyle.defaultNormal(),
    var hover: UiStyle = UiStyle.defaultHover(),
    var focus: UiStyle = UiStyle.defaultFocus(),
) {
    fun cpy() = UiStyleSheet(normal,hover,focus)
}