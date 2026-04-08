package com.voxd31.editor.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import com.voxd31.editor.CameraMode
import com.voxd31.editor.OrthographicView

class VoxcraftUiOverlay(
    private val toolNamesProvider: () -> List<String>,
    private val activeToolIndexProvider: () -> Int,
    private val toolSelected: (Int) -> Unit,
    private val currentColorProvider: () -> Color,
    private val colorSelected: (Color) -> Unit,
    private val addModeProvider: () -> String,
    private val addModeChanged: (String) -> Unit,
    private val cameraModeProvider: () -> CameraMode,
    private val cameraModeChanged: (CameraMode) -> Unit,
    private val orthographicViewChanged: (OrthographicView) -> Unit,
    private val saveAction: () -> Unit,
    private val clearSelectionAction: () -> Unit,
    private val clearGuidesAction: () -> Unit,
    private val resetToolAction: () -> Unit,
    private val statusProvider: () -> StatusSnapshot
) {
    data class StatusSnapshot(
        val fileName: String,
        val cameraMode: String,
        val activeTool: String,
        val cubeCount: Int,
        val selectionCount: Int,
        val guideCount: Int,
        val addMode: String,
        val cursor: String
    )

    val stage: Stage = Stage(ScreenViewport())

    private val root = Table()
    private val whiteTexture: Texture = createWhiteTexture()
    private val toolGroup = ButtonGroup<VisTextButton>()
    private val toolButtons = mutableListOf<VisTextButton>()
    private val cameraButtons = linkedMapOf<CameraMode, VisTextButton>()
    private val addModeSelect = VisSelectBox<String>()
    private val currentColorPreview = ColorChip(whiteTexture) { currentColorProvider() }
    private val colorSwatches = mutableListOf<ColorChip>()
    private val fileLabel = VisLabel("")
    private val cameraLabel = VisLabel("")
    private val toolLabel = VisLabel("")
    private val countsLabel = VisLabel("")
    private val cursorLabel = VisLabel("")
    private var syncing = false

    init {
        toolGroup.setMinCheckCount(1)
        toolGroup.setMaxCheckCount(1)
        toolGroup.setUncheckLast(true)

        root.setFillParent(true)
        root.top().left()
        root.touchable = Touchable.childrenOnly
        stage.addActor(root)

        val topBar = buildTopBar()
        val toolsWindow = buildToolsWindow()
        val colorsWindow = buildColorsWindow()
        val statusWindow = buildStatusWindow()

        root.add(topBar).growX().colspan(3).pad(8f, 8f, 4f, 8f)
        root.row()
        root.add(toolsWindow).width(220f).top().left().padLeft(8f).padBottom(8f)
        root.add().expand()
        root.add(colorsWindow).width(320f).top().right().padRight(8f).padBottom(8f)
        root.row()
        root.add(statusWindow).growX().colspan(3).pad(0f, 8f, 8f, 8f)

        rebuildToolButtons()
        syncFromState()
    }

    fun act(delta: Float) {
        syncFromState()
        stage.act(delta)
    }

    fun draw() {
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    fun isPointerOverUi(screenX: Int, screenY: Int): Boolean {
        val stageCoords = stage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))
        return stage.hit(stageCoords.x, stageCoords.y, true) != null
    }

    fun releaseScrollFocusIfPointerOutside(screenX: Int, screenY: Int) {
        if (!isPointerOverUi(screenX, screenY)) {
            stage.scrollFocus = null
        }
    }

    fun dispose() {
        whiteTexture.dispose()
        stage.dispose()
    }

    private fun buildTopBar(): VisWindow {
        val window = fixedWindow("Editor")
        val content = VisTable(true)

        val saveButton = VisTextButton("Save")
        saveButton.addListener(onChange { saveAction() })
        content.add(saveButton).padRight(6f)

        val resetButton = VisTextButton("Reset Tool")
        resetButton.addListener(onChange { resetToolAction() })
        content.add(resetButton).padRight(6f)

        val clearSelectionButton = VisTextButton("Clear Selection")
        clearSelectionButton.addListener(onChange { clearSelectionAction() })
        content.add(clearSelectionButton).padRight(6f)

        val clearGuidesButton = VisTextButton("Clear Guides")
        clearGuidesButton.addListener(onChange { clearGuidesAction() })
        content.add(clearGuidesButton).padRight(12f)

        content.add(VisLabel("Add")).padRight(4f)
        addModeSelect.setItems("addWithoutReplace", "addOrReplace", "replaceCube")
        addModeSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (!syncing) {
                    addModeChanged(addModeSelect.selected)
                }
            }
        })
        content.add(addModeSelect).width(180f).padRight(12f)

        content.add(VisLabel("Camera")).padRight(4f)
        val cameraGroup = ButtonGroup<VisTextButton>().apply {
            setMinCheckCount(1)
            setMaxCheckCount(1)
            setUncheckLast(true)
        }
        CameraMode.entries.forEach { mode ->
            val button = VisTextButton(mode.displayName, "toggle")
            button.addListener(onChange { cameraModeChanged(mode) })
            cameraButtons[mode] = button
            cameraGroup.add(button)
            content.add(button).padRight(4f)
        }

        content.add(VisLabel("View")).padLeft(8f).padRight(4f)
        listOf(
            OrthographicView.TOP,
            OrthographicView.FRONT,
            OrthographicView.RIGHT,
            OrthographicView.LEFT,
            OrthographicView.BACK
        ).forEach { view ->
            val button = VisTextButton(view.displayName)
            button.addListener(onChange { orthographicViewChanged(view) })
            content.add(button).padRight(4f)
        }

        window.add(content).growX().left().pad(6f)
        window.pack()
        return window
    }

    private fun buildToolsWindow(): VisWindow {
        val window = fixedWindow("Tools")
        val content = VisTable(true)
        val scroll = VisScrollPane(content)
        scroll.setFadeScrollBars(false)
        scroll.setScrollingDisabled(true, false)
        window.add(scroll).grow().minHeight(320f)
        window.userObject = content
        return window
    }

    private fun buildColorsWindow(): VisWindow {
        val window = fixedWindow("Palette")
        val content = VisTable(true)

        val previewRow = VisTable(true)
        previewRow.add(VisLabel("Current")).left().padRight(8f)
        previewRow.add(currentColorPreview).size(42f, 24f).left().padRight(8f)
        content.add(previewRow).left().growX()
        content.row()

        addPaletteSection(content, "Primary", buildPaletteRows(primaryPalette(), 6))
        addPaletteSection(content, "Transparent", buildPaletteRows(transparentPalette(), 6))
        addPaletteSection(content, "Grayscale", buildPaletteRows(grayPalette(), 5))

        window.add(content).grow().pad(6f)
        window.pack()
        return window
    }

    private fun buildStatusWindow(): VisWindow {
        val window = fixedWindow("Status")
        val content = VisTable(true)

        fileLabel.setAlignment(Align.left)
        cameraLabel.setAlignment(Align.left)
        toolLabel.setAlignment(Align.left)
        countsLabel.setAlignment(Align.left)
        cursorLabel.setAlignment(Align.left)
        cursorLabel.setWrap(true)

        content.add(fileLabel).growX().left()
        content.row()
        content.add(cameraLabel).growX().left()
        content.row()
        content.add(toolLabel).growX().left()
        content.row()
        content.add(countsLabel).growX().left()
        content.row()
        content.add(cursorLabel).growX().left().minHeight(44f)

        window.add(content).growX().pad(6f)
        window.pack()
        return window
    }

    private fun rebuildToolButtons() {
        val toolsContent = root.findActor<VisWindow>("Tools")
        val content = toolsContent.userObject as VisTable
        content.clearChildren()
        toolButtons.clear()
        toolGroup.buttons.clear()

        toolNamesProvider().forEachIndexed { index, toolName ->
            val button = VisTextButton(toolName, "toggle")
            button.label.setWrap(true)
            button.addListener(onChange {
                if (!syncing && button.isChecked) {
                    toolSelected(index)
                }
            })
            toolGroup.add(button)
            toolButtons += button
            content.add(button).growX().left().padBottom(4f)
            content.row()
        }
    }

    private fun addPaletteSection(parent: VisTable, title: String, rows: List<List<Color>>) {
        parent.add(VisLabel(title)).left().padTop(8f).padBottom(4f)
        parent.row()
        rows.forEach { row ->
            val rowTable = VisTable(true)
            row.forEach { color ->
                val chip = ColorChip(whiteTexture) { color }.apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            colorSelected(color.cpy())
                        }
                    })
                }
                colorSwatches += chip
                rowTable.add(chip).size(28f).pad(1f)
            }
            parent.add(rowTable).left()
            parent.row()
        }
    }

    private fun syncFromState() {
        syncing = true
        val snapshot = statusProvider()
        val currentColor = currentColorProvider()

        addModeSelect.selected = addModeProvider()
        cameraButtons.forEach { (mode, button) ->
            button.isChecked = cameraModeProvider() == mode
        }
        toolButtons.forEachIndexed { index, button ->
            button.isChecked = activeToolIndexProvider() == index
        }
        colorSwatches.forEach { swatch ->
            swatch.selected = colorMatches(currentColor, swatch.colorProvider())
        }
        currentColorPreview.selected = false

        fileLabel.setText("File: ${snapshot.fileName}")
        cameraLabel.setText("Camera: ${snapshot.cameraMode} | Add: ${snapshot.addMode}")
        toolLabel.setText("Tool: ${snapshot.activeTool}")
        countsLabel.setText(
            "Cubes: ${snapshot.cubeCount} | Selection: ${snapshot.selectionCount} | Guides: ${snapshot.guideCount}"
        )
        cursorLabel.setText(snapshot.cursor)
        syncing = false
    }

    private fun fixedWindow(title: String): VisWindow {
        return VisWindow(title).apply {
            setName(title)
            isMovable = false
            isResizable = false
            isModal = false
            setKeepWithinParent(false)
        }
    }

    private fun buildPaletteRows(colors: List<Color>, rowSize: Int): List<List<Color>> {
        return colors.chunked(rowSize)
    }

    private fun primaryPalette(): List<Color> {
        return (0 until 36).map { index ->
            Color().apply {
                fromHsv(index * 10f, 1f, 1f)
                a = 1f
            }
        }
    }

    private fun transparentPalette(): List<Color> {
        return (0 until 24).map { index ->
            Color().apply {
                fromHsv(index * 15f, 0.9f, 1f)
                a = 0.55f
            }
        }
    }

    private fun grayPalette(): List<Color> {
        return (0..10).map { index ->
            val value = index / 10f
            Color(value, value, value, 1f)
        }
    }

    private fun colorMatches(a: Color, b: Color): Boolean {
        return kotlin.math.abs(a.r - b.r) < 0.01f &&
            kotlin.math.abs(a.g - b.g) < 0.01f &&
            kotlin.math.abs(a.b - b.b) < 0.01f &&
            kotlin.math.abs(a.a - b.a) < 0.01f
    }

    private fun createWhiteTexture(): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        return Texture(pixmap).also { pixmap.dispose() }
    }

    private fun onChange(action: () -> Unit): ChangeListener {
        return object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (!syncing) {
                    action()
                }
            }
        }
    }

    private class ColorChip(
        private val texture: Texture,
        val colorProvider: () -> Color
    ) : Actor() {
        var selected: Boolean = false

        init {
            setSize(28f, 28f)
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            val base = colorProvider()
            val original = batch.color.cpy()
            batch.color = Color(base.r, base.g, base.b, base.a * parentAlpha)
            batch.draw(texture, x, y, width, height)

            val outline = if (selected) Color(1f, 0.85f, 0.2f, parentAlpha) else Color(0f, 0f, 0f, 0.55f * parentAlpha)
            batch.color = outline
            val thickness = if (selected) 3f else 1f
            batch.draw(texture, x, y, width, thickness)
            batch.draw(texture, x, y + height - thickness, width, thickness)
            batch.draw(texture, x, y, thickness, height)
            batch.draw(texture, x + width - thickness, y, thickness, height)
            batch.color = original
        }

        override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
            return if (x >= 0f && x <= width && y >= 0f && y <= height) this else null
        }
    }
}
