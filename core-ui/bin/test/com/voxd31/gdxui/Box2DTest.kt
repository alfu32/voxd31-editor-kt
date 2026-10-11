package com.voxd31.gdxui

import com.badlogic.gdx.math.Vector2
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class Box2DTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun testConstructorAndDefaults() {
        val box = Box2D()
        assertEquals(Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), box.min)
        assertEquals(Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY), box.max)
        assertEquals(Vector2(0f, 0f), box.getCenter())
        assertEquals(Float.POSITIVE_INFINITY, box.getWidth(), 0.0f)
        assertEquals(Float.POSITIVE_INFINITY, box.getHeight(), 0.0f)
    }

    @Test
    fun testOfVector2() {
        val vector = Vector2(1f, 2f)
        val box = Box2D.of(vector)
        assertEquals(vector, box.min)
        assertEquals(vector, box.max)
        println(box.getCenter())
    }

    @Test
    fun testOfBox2D() {
        val box1 = Box2D()
        box1.set(Vector2(1f, 2f),Vector2(3f, 4f))
        val box2 = Box2D.of(box1)
        assertEquals(box1.min, box2.min)
        assertEquals(box1.max, box2.max)
    }

    @Test
    fun testSet() {
        val box = Box2D()
        val min = Vector2(1f, 2f)
        val max = Vector2(3f, 4f)
        box.set(min, max)
        assertEquals(min, box.min)
        assertEquals(max, box.max)
    }

    @Test
    fun testUpdate() {
        val box = Box2D()
        val min = Vector2(1f, 2f)
        val max = Vector2(3f, 4f)
        box.set(min, max)
        assertEquals(Vector2(2f, 3f), box.getCenter())
        assertEquals(2f, box.getWidth(), 0.0f)
        assertEquals(2f, box.getHeight(), 0.0f)
    }

    @Test
    fun testInf() {
        val box = Box2D.of(Vector2(1f, 2f))
        box.inf()
        assertEquals(Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), box.min)
        assertEquals(Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY), box.max)
    }

    @Test
    fun testExtVector2() {
        val box = Box2D.of(Vector2(1f, 2f))
        box.ext(Vector2(3f, 4f))
        assertEquals(Vector2(1f, 2f), box.min)
        assertEquals(Vector2(3f, 4f), box.max)
    }

    @Test
    fun testExtBox2D() {
        val box1 = Box2D.of(Vector2(1f, 2f))
        val box2 = Box2D.of(Vector2(3f, 4f))
        box1.ext(box2)
        assertEquals(Vector2(1f, 2f), box1.min)
        assertEquals(Vector2(3f, 4f), box1.max)
    }

    @Test
    fun testContainsVector2() {
        val box = Box2D.of(Vector2(1f, 2f))
        box.ext(Vector2(3f, 4f))
        assertTrue(box.contains(Vector2(2f, 3f)))
        assertFalse(box.contains(Vector2(0f, 0f)))
    }

    @Test
    fun testContainsBox2D() {
        val box1 = Box2D.of(Vector2(1f, 2f)).ext(Vector2(3f, 4f))
        val box2 = Box2D.of(Vector2(2f, 3f)).ext(Vector2(2.5f, 3.5f))
        assertTrue(box1.contains(box2))
        assertFalse(box2.contains(box1))
    }

    @Test
    fun testIntersects() {
        val box1 = Box2D.of(Vector2(1f, 2f)).ext(Vector2(3f, 4f))
        val box2 = Box2D.of(Vector2(2f, 3f)).ext(Vector2(4f, 5f))
        assertTrue(box1.intersects(box2))
        val box3 = Box2D.of(Vector2(5f, 6f)).ext(Vector2(7f, 8f))
        assertFalse(box1.intersects(box3))
    }

    @Test
    fun testClr() {
        val box = Box2D.of(Vector2(1f, 2f))
        box.clr()
        assertEquals(Vector2(0f, 0f), box.min)
        assertEquals(Vector2(0f, 0f), box.max)
    }

    @Test
    fun testIsValid() {
        val box = Box2D()
        assertFalse(box.isValid())
        box.set(Vector2(1f, 2f), Vector2(3f, 4f))
        assertTrue(box.isValid())
    }

    @Test
    fun testToString() {
        val box = Box2D.of(Vector2(1f, 2f))
        assertEquals("[(1.0,2.0)|(1.0,2.0)]", box.toString())
    }
}