package com.voxd31.gdxui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

class UIFont(
    var ttf:String,
    var size:Int,
    var color:Color,
){
    fun fontId(): String {
        return "$ttf ${size}px #${color.toIntBits().toString(16).uppercase().padStart(8, '0')}"
    }
    fun bitmapFont():BitmapFont{
        val key = fontId()
        if (!fonts.containsKey(key)) {
            fonts[key] = generateFont(ttf, size, color)
        }
        return UIFont.fonts[key]!!
    }
    companion object {
        val fonts: HashMap<String, BitmapFont> = hashMapOf()
        init {
            fonts["default"]=default().bitmapFont()
        }
        private fun generateFont(filePath: String, size: Int, color: Color = Color.DARK_GRAY): BitmapFont {
            val generator = FreeTypeFontGenerator(Gdx.files.internal(filePath))
            val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = size
                this.color = color
            }
            val font = generator.generateFont(parameter)
            generator.dispose()  // Don't forget to dispose to avoid memory leaks
            return font
        }
        fun default():UIFont{
            return UIFont.of("NotoSans-Regular 16px 000000FF")
        }
        fun of(def:String):UIFont{
            val tk=def.split(" ")
            return UIFont(
                "${tk[0]}.ttf",
                tk[1].replace("px","").toInt(),
                Color.valueOf(tk[2]),
            )
        }
    }

}

class UiStyle(
    var color: Color = Color.BLACK,
    var background: Color = Color.WHITE,
    var border: Color= Color.BLUE,
    var font:UIFont=UIFont.default(),
) {
    companion object{

        fun defaultNormal() = UiStyle(
            color=Color.valueOf("222222ff"),
            background = Color(0x090809ff),
            border = Color(0x090809ff),
            font = UIFont.of("NotoSans-Regular 16px EEEEEEFF"),
        )
        fun defaultHover() = UiStyle(
            color=Color.valueOf("eeffffff"),
            background = Color(0x0f0e0fff),
            border = Color.CYAN,
            font = UIFont("NotoSans-Regular.ttf",16,Color.CYAN),
        )
        fun defaultFocus() = UiStyle(
            color=Color.valueOf("333333ff"),
            background = Color(0x090809ff),
            border = Color.GOLD,
            font = UIFont("NotoSans-Regular.ttf",16,Color.GOLD),
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