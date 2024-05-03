package com.voxd31.gdxui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvmhost.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class Vox3Event(
    var keyCode: Int? = null,
    var keyDown: Int? = null,
    var screen: Vector2? = null,
    var scroll: Vector2? = null,
    var modelPoint: Vector3? = null,
    var modelVoxel: Vector3? = null,
    var modelNextPoint: Vector3? = null,
    var modelNextVoxel: Vector3? = null,
    var target:Cube? = null,
    var normal: Vector3? = null,
    var pointer:Int? = null,
    var button:Int? = null,
    var channel:String = "none"
) {
    override fun toString(): String {
        return "keyDown:$keyDown , screen:$screen , model:$modelPoint ,modelNext:$modelNextPoint , pointer:$pointer , button:$button , target:${target?.getId()}"
    }
}

typealias EventListener = (e:Vox3Event)->Unit

interface Shape {
    fun draw(batch: Batch)
    fun addEventListener(type: String, listener: (Element, Vox3Event) -> Unit)
}

class Circle(var x: Float, var y: Float, var radius: Float) : Shape {
    private val listeners = mutableMapOf<String, (Element, Vox3Event) -> Unit>()

    override fun draw(batch: Batch) {
        // Use libGDX to draw a circle
    }

    override fun addEventListener(type: String, listener: (Element, Vox3Event) -> Unit) {
        listeners[type] = listener
    }
}

class Rect(var x: Float, var y: Float, var width: Float, var height: Float) : Shape {
    private val listeners = mutableMapOf<String, (Element, Vox3Event) -> Unit>()

    override fun draw(batch: Batch) {
        // Use libGDX to draw a rectangle
    }

    override fun addEventListener(type: String, listener: (Element, Vox3Event) -> Unit) {
        listeners[type] = listener
    }
}

class Composite : Shape {
    private val children = mutableListOf<Shape>()
    private val listeners = mutableMapOf<String, (Element, Vox3Event) -> Unit>()

    fun addShape(shape: Shape) {
        children.add(shape)
    }

    override fun draw(batch: Batch) {
        for (child in children) {
            child.draw(batch)
        }
    }

    override fun addEventListener(type: String, listener: (Element, Vox3Event) -> Unit) {
        listeners[type] = listener
    }
}
fun executeScript(script: String) {
    val configuration = ScriptCompilationConfiguration {
        // Configuration details here
    }
    val evalConfig = ScriptEvaluationConfiguration {
        // Evaluation configurations here
    }

    /// TODO fixme val compiler = JvmScriptCompiler()
    /// TODO fixme val result = compiler.compile(ScriptSource.Dynamic(script), configuration)
    /// TODO fixme     .onSuccess { compiledScript ->
    /// TODO fixme         compiler.eval(compiledScript, evalConfig)
    /// TODO fixme     }
}

class GdxRenderer(private val scriptSource: String) : ApplicationAdapter() {
    private lateinit var batch: Batch
    private lateinit var document: Composite

    override fun create() {
        batch = SpriteBatch()
        document = parseScriptSource(scriptSource)
        executeScript(findScriptContents())
    }

    private fun parseScriptSource(source: String): Composite {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val inputStream = source.byteInputStream()
        val doc = dBuilder.parse(inputStream)

        val composite = Composite()

        val rootNode = doc.getElementsByTagName("document").item(0)
        (0 .. rootNode.childNodes.length).forEach { childIndex ->
            val node = rootNode.childNodes.item(childIndex)
            if (node is Element) {
                composite.addShape(parseElement(node))
            }
        }

        return composite
    }

    private fun parseElement(node: Element): Shape {
        return when (node.tagName.toLowerCase()) {
            "circle" -> Circle(
                x = node.getAttribute("x").toFloat(),
                y = node.getAttribute("y").toFloat(),
                radius = node.getAttribute("radius").toFloat()
            )
            "rect" -> Rect(
                x = node.getAttribute("x").toFloat(),
                y = node.getAttribute("y").toFloat(),
                width = node.getAttribute("width").toFloat(),
                height = node.getAttribute("height").toFloat()
            )
            "composite" -> {
                val composite = Composite()
                (0 .. node.childNodes.length).forEach { childIndex ->
                    val childNode = node.childNodes.item(childIndex)
                    if (childNode is Element) {
                        composite.addShape(parseElement(childNode))
                    }
                }
                composite
            }
            else -> throw IllegalArgumentException("Unsupported shape type: ${node.tagName}")
        }
    }

    private fun findScriptContents(): String {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val inputStream = scriptSource.byteInputStream()
        val doc = dBuilder.parse(inputStream)

        val scriptNode = doc.getElementsByTagName("script").item(0) as Element
        return scriptNode.textContent.trim()
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        document.draw(batch)
    }

    override fun dispose() {
        batch.dispose()
    }
}