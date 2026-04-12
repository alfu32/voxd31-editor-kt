package com.voxd31.editor.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.color.ColorPicker
import com.kotcrab.vis.ui.widget.color.ColorPickerListener
import com.voxd31.editor.CameraMode
import com.voxd31.editor.ModelSettings
import com.voxd31.editor.OrthographicView
import com.voxd31.editor.ToolOperator

class VoxcraftUiOverlay(
    private val toolNamesProvider: () -> List<String>,
    private val activeToolIndexProvider: () -> Int,
    private val toolSelected: (Int) -> Unit,
    private val toolOperatorsProvider: () -> List<ToolOperator>,
    private val currentColorProvider: () -> Color,
    private val colorSelected: (Color) -> Unit,
    private val addModeProvider: () -> String,
    private val addModeChanged: (String) -> Unit,
    private val cameraModeProvider: () -> CameraMode,
    private val cameraModeChanged: (CameraMode) -> Unit,
    private val orthographicViewChanged: (OrthographicView) -> Unit,
    private val openAction: () -> Unit,
    private val saveAction: () -> Unit,
    private val saveAsAction: () -> Unit,
    private val exportChoicesProvider: () -> List<String>,
    private val exportChoiceSelected: (String) -> Unit,
    private val modelSettingsProvider: () -> ModelSettings,
    private val modelSettingsChanged: (ModelSettings) -> Unit,
    private val deleteSelectionAction: () -> Unit,
    private val clearSelectionAction: () -> Unit,
    private val clearGuidesAction: () -> Unit,
    private val resetToolAction: () -> Unit,
    private val uiScaleProvider: () -> Float,
    private val uiScaleChanged: (Float) -> Unit,
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
        val message: String,
        val cursor: String
    )

    private val uiViewport = ScreenViewport()
    val stage: Stage = Stage(uiViewport)

    private val root = Table()
    private val whiteTexture: Texture = createWhiteTexture()
    private val toolGroup = ButtonGroup<VisTextButton>()
    private val toolButtons = linkedMapOf<Int, VisTextButton>()
    private val cameraButtons = linkedMapOf<CameraMode, VisTextButton>()
    private val addModeSelect = VisSelectBox<String>()
    private val uiScaleSelect = VisSelectBox<String>()
    private val modificationToolsContent = VisTable(true)
    private val constructionToolsContent = VisTable(true)
    private val toolOperatorsContent = VisTable(true)
    private val currentColorPreview = ColorChip(whiteTexture) { currentColorProvider() }.apply {
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showColorPicker()
            }
        })
    }
    private val fileLabel = VisLabel("")
    private val cameraLabel = VisLabel("")
    private val toolLabel = VisLabel("")
    private val countsLabel = VisLabel("")
    private val messageLabel = VisLabel("")
    private val cursorLabel = VisLabel("")
    private var colorPicker: ColorPicker? = null
    private var modelSettingsDialog: VisWindow? = null
    private var exportDialog: VisWindow? = null
    private var toolOperatorsSignature = ""
    private var syncing = false

    init {
        setUiScale(uiScaleProvider())
        toolGroup.setMinCheckCount(1)
        toolGroup.setMaxCheckCount(1)
        toolGroup.setUncheckLast(true)

        root.setFillParent(true)
        root.top().left()
        root.touchable = Touchable.childrenOnly
        stage.addActor(root)

        val topBar = buildTopBar()
        val toolsColumn = VisTable(true).apply {
            add(buildToolsWindow("Modification", modificationToolsContent)).width(220f).growX().top().left()
            row()
            add(buildToolsWindow("Construction", constructionToolsContent)).width(220f).grow().top().left()
        }
        val toolOperatorsWindow = buildToolOperatorsWindow()
        val statusWindow = buildStatusWindow()

        root.add(topBar).growX().colspan(3).pad(8f, 8f, 4f, 8f)
        root.row()
        root.add(toolsColumn).width(220f).top().left().padLeft(8f).padBottom(8f)
        root.add().expand()
        root.add(toolOperatorsWindow).top().right().padRight(8f).padTop(4f)
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
        uiViewport.update(width, height, true)
    }

    fun setUiScale(scale: Float) {
        val normalized = normalizeUiScale(scale)
        uiViewport.setUnitsPerPixel(1f / normalized)
        uiViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        root.invalidateHierarchy()
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

        val openButton = VisTextButton("Open")
        openButton.addListener(onChange { openAction() })
        content.add(openButton).padRight(6f)

        val saveButton = VisTextButton("Save")
        saveButton.addListener(onChange { saveAction() })
        content.add(saveButton).padRight(6f)

        val saveAsButton = VisTextButton("Save As")
        saveAsButton.addListener(onChange { saveAsAction() })
        content.add(saveAsButton).padRight(6f)

        val exportButton = VisTextButton("Export")
        exportButton.addListener(onChange { showExportDialog() })
        content.add(exportButton).padRight(6f)

        val modelButton = VisTextButton("Model")
        modelButton.addListener(onChange { showModelSettingsDialog() })
        content.add(modelButton).padRight(6f)

        val resetButton = VisTextButton("Reset Tool")
        resetButton.addListener(onChange { resetToolAction() })
        content.add(resetButton).padRight(6f)

        val deleteButton = VisTextButton("Delete")
        deleteButton.addListener(onChange { deleteSelectionAction() })
        content.add(deleteButton).padRight(6f)

        val clearSelectionButton = VisTextButton("Clear Selection")
        clearSelectionButton.addListener(onChange { clearSelectionAction() })
        content.add(clearSelectionButton).padRight(6f)

        val clearGuidesButton = VisTextButton("Clear Guides")
        clearGuidesButton.addListener(onChange { clearGuidesAction() })
        content.add(clearGuidesButton).padRight(6f)

        content.add(currentColorPreview).size(32f).padRight(12f)

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

        content.add(VisLabel("UI")).padRight(4f)
        uiScaleSelect.setItems("1x", "1.5x", "2x")
        uiScaleSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (!syncing) {
                    uiScaleChanged(parseUiScaleLabel(uiScaleSelect.selected))
                }
            }
        })
        content.add(uiScaleSelect).width(80f).padRight(12f)

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

    private fun showExportDialog() {
        val choices = exportChoicesProvider()
        exportDialog?.remove()
        val dialog = fixedWindow("Export").also {
            it.isModal = true
            exportDialog = it
        }

        val content = VisTable(true)
        if (choices.isEmpty()) {
            content.add(VisLabel("No export formats are available.")).pad(10f)
            val closeButton = VisTextButton("Close")
            closeButton.addListener(onChange { dialog.remove() })
            dialog.add(content).pad(10f)
            dialog.row()
            dialog.add(closeButton).pad(0f, 10f, 10f, 10f).right()
        } else {
            val select = VisSelectBox<String>()
            select.setItems(*choices.toTypedArray())
            content.add(VisLabel("Format")).left().padRight(8f)
            content.add(select).width(280f).left()

            val buttons = VisTable(true)
            val exportButton = VisTextButton("Export")
            exportButton.addListener(onChange {
                val selected = select.selected
                dialog.remove()
                exportChoiceSelected(selected)
            })
            val cancelButton = VisTextButton("Cancel")
            cancelButton.addListener(onChange { dialog.remove() })
            buttons.add(exportButton).padRight(6f)
            buttons.add(cancelButton)

            dialog.add(content).pad(10f)
            dialog.row()
            dialog.add(buttons).pad(0f, 10f, 10f, 10f).right()
        }

        if (dialog.stage == null) {
            stage.addActor(dialog)
        }
        dialog.pack()
        dialog.centerWindow()
        dialog.fadeIn()
    }

    private fun buildToolsWindow(title: String, content: VisTable): VisWindow {
        val window = fixedWindow(title)
        val scroll = VisScrollPane(content)
        scroll.setFadeScrollBars(false)
        scroll.setScrollingDisabled(true, false)
        window.add(scroll).grow().minHeight(if (title == "Modification") 180f else 320f)
        return window
    }

    private fun buildToolOperatorsWindow(): VisWindow {
        val window = fixedWindow("In-Tool Operators")
        val scroll = VisScrollPane(toolOperatorsContent)
        scroll.setFadeScrollBars(false)
        scroll.setScrollingDisabled(true, false)
        window.add(scroll).width(220f).minHeight(80f).pad(6f)
        return window
    }

    private fun buildStatusWindow(): VisWindow {
        val window = fixedWindow("Status")
        val content = VisTable(true)

        fileLabel.setAlignment(Align.left)
        cameraLabel.setAlignment(Align.left)
        toolLabel.setAlignment(Align.left)
        countsLabel.setAlignment(Align.left)
        messageLabel.setAlignment(Align.left)
        cursorLabel.setAlignment(Align.left)
        messageLabel.setWrap(true)
        cursorLabel.setWrap(true)

        content.add(fileLabel).growX().left()
        content.row()
        content.add(cameraLabel).growX().left()
        content.row()
        content.add(toolLabel).growX().left()
        content.row()
        content.add(countsLabel).growX().left()
        content.row()
        content.add(messageLabel).growX().left().minHeight(24f)
        content.row()
        content.add(cursorLabel).growX().left().minHeight(44f)

        window.add(content).growX().pad(6f)
        window.pack()
        return window
    }

    private fun rebuildToolButtons() {
        modificationToolsContent.clearChildren()
        constructionToolsContent.clearChildren()
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
            toolButtons[index] = button

            val content = if (isModificationTool(toolName)) modificationToolsContent else constructionToolsContent
            content.add(button).growX().left().padBottom(4f)
            content.row()
        }
    }

    private fun isModificationTool(toolName: String): Boolean {
        return toolName.lowercase() in setOf("select2", "select", "move", "copy", "rotate", "copyrot")
    }

    private fun showColorPicker() {
        if (colorPicker == null) {
            colorPicker = ColorPicker("Color").apply {
                setListener(object : ColorPickerListener {
                    override fun changed(color: Color?) {
                        if (color != null) {
                            colorSelected(Color(color))
                        }
                    }

                    override fun canceled(oldColor: Color?) {
                        if (oldColor != null) {
                            colorSelected(Color(oldColor))
                        }
                    }

                    override fun reset(oldColor: Color?, newColor: Color?) {
                        if (newColor != null) {
                            colorSelected(Color(newColor))
                        }
                    }

                    override fun finished(color: Color?) {
                        if (color != null) {
                            colorSelected(Color(color))
                        }
                    }
                })
            }
        }
        val picker = colorPicker ?: return
        picker.color = Color(currentColorProvider())
        if (picker.stage == null) {
            stage.addActor(picker)
        }
        picker.centerWindow()
        picker.fadeIn()
    }

    private fun showModelSettingsDialog() {
        val current = modelSettingsProvider()
        modelSettingsDialog?.remove()
        val dialog = fixedWindow("Model Settings").also {
            it.isModal = true
            modelSettingsDialog = it
        }

        val content = VisTable(true)
        val gridField = VisTextField(current.gridSize.toString())
        val unitSizeField = VisTextField(current.unitSize.toString())
        val unitSuffixField = VisTextField(current.unitSuffix)

        content.add(VisLabel("Grid Size")).left().padRight(8f)
        content.add(gridField).width(160f).left()
        content.row()
        content.add(VisLabel("Unit Size")).left().padRight(8f)
        content.add(unitSizeField).width(160f).left()
        content.row()
        content.add(VisLabel("Unit Suffix")).left().padRight(8f)
        content.add(unitSuffixField).width(160f).left()

        val buttons = VisTable(true)
        val applyButton = VisTextButton("Apply")
        applyButton.addListener(onChange {
            val gridSize = gridField.text.toIntOrNull()?.coerceAtLeast(1) ?: current.gridSize
            val unitSize = unitSizeField.text.toFloatOrNull()?.coerceAtLeast(1e-6f) ?: current.unitSize
            val unitSuffix = unitSuffixField.text.ifBlank { current.unitSuffix }
            modelSettingsChanged(ModelSettings(gridSize = gridSize, unitSize = unitSize, unitSuffix = unitSuffix))
            dialog.remove()
        })
        val cancelButton = VisTextButton("Cancel")
        cancelButton.addListener(onChange { dialog.remove() })
        buttons.add(applyButton).padRight(6f)
        buttons.add(cancelButton)

        dialog.add(content).pad(10f)
        dialog.row()
        dialog.add(buttons).pad(0f, 10f, 10f, 10f).right()

        if (dialog.stage == null) {
            stage.addActor(dialog)
        }
        dialog.pack()
        dialog.centerWindow()
        dialog.fadeIn()
    }

    private fun syncFromState() {
        syncing = true
        val snapshot = statusProvider()

        addModeSelect.selected = addModeProvider()
        uiScaleSelect.selected = uiScaleLabel(uiScaleProvider())
        cameraButtons.forEach { (mode, button) ->
            button.isChecked = cameraModeProvider() == mode
        }
        toolButtons.forEach { (index, button) ->
            button.isChecked = activeToolIndexProvider() == index
        }
        currentColorPreview.selected = false

        fileLabel.setText("File: ${snapshot.fileName}")
        cameraLabel.setText("Camera: ${snapshot.cameraMode} | Add: ${snapshot.addMode}")
        toolLabel.setText("Tool: ${snapshot.activeTool}")
        countsLabel.setText(
            "Cubes: ${snapshot.cubeCount} | Selection: ${snapshot.selectionCount} | Guides: ${snapshot.guideCount}"
        )
        messageLabel.setText("Message: ${snapshot.message}")
        cursorLabel.setText(snapshot.cursor)
        rebuildToolOperatorsIfNeeded(snapshot.activeTool)
        syncing = false
    }

    private fun rebuildToolOperatorsIfNeeded(toolName: String) {
        val operators = toolOperatorsProvider()
        val signature = buildString {
            append(toolName)
            operators.forEach { operator ->
                append('|')
                append(operator.label)
                append(':')
                append(operator.active())
                append(':')
                append(operator.enabled())
            }
        }
        if (signature == toolOperatorsSignature) {
            return
        }
        toolOperatorsSignature = signature
        toolOperatorsContent.clearChildren()

        val activeToolName = VisTextButton(toolName.ifBlank { "-" }, "toggle")
        activeToolName.isDisabled = true
        activeToolName.label.setWrap(true)
        toolOperatorsContent.add(activeToolName).growX().left().padBottom(6f)
        toolOperatorsContent.row()

        operators.forEach { operator ->
            val label = if (operator.active()) "* ${operator.label}" else operator.label
            val button = VisTextButton(label)
            button.isDisabled = !operator.enabled()
            button.label.setWrap(true)
            button.addListener(onChange {
                if (operator.enabled()) {
                    operator.action()
                    toolOperatorsSignature = ""
                }
            })
            toolOperatorsContent.add(button).growX().left().padBottom(4f)
            toolOperatorsContent.row()
        }
        toolOperatorsContent.invalidateHierarchy()
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

    private fun uiScaleLabel(scale: Float): String {
        return when (normalizeUiScale(scale)) {
            1.5f -> "1.5x"
            2f -> "2x"
            else -> "1x"
        }
    }

    private fun parseUiScaleLabel(label: String?): Float {
        return when (label) {
            "1.5x" -> 1.5f
            "2x" -> 2f
            else -> 1f
        }
    }

    private fun normalizeUiScale(scale: Float): Float {
        return when {
            scale >= 1.75f -> 2f
            scale >= 1.25f -> 1.5f
            else -> 1f
        }
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

            val outline = if (selected) {
                Color(1f, 0.85f, 0.2f, parentAlpha)
            } else {
                Color(0f, 0f, 0f, 0.55f * parentAlpha)
            }
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
