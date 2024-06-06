package com.voxd31.gdxui

import com.badlogic.gdx.graphics.Color
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class UiStyleTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun getTextColorTest() {
        println("dummy test passed getTextColor")
    }

    @Test
    fun setTextColorTest() {
        println("dummy test passed setTextColor")
    }
    @Test
    fun getFontIdTest() {
        val key = UIFont("NotoSans-Regular.ttf",12, Color.BLUE).fontId()
        println(key)
    }
}