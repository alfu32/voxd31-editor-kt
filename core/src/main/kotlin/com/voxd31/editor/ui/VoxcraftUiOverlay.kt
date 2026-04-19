package com.voxd31.editor.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.color.ColorPicker
import com.kotcrab.vis.ui.widget.color.ColorPickerListener
import com.voxd31.editor.CameraInteractionMode
import com.voxd31.editor.CameraMode
import com.voxd31.editor.ModelSettings
import com.voxd31.editor.OrthographicView
import com.voxd31.editor.ToolOperator
import kotlin.math.max

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
    private val cameraInteractionModeProvider: () -> CameraInteractionMode,
    private val cameraInteractionModeChanged: (CameraInteractionMode) -> Unit,
    private val orthographicViewChanged: (OrthographicView) -> Unit,
    private val openAction: () -> Unit,
    private val importAction: () -> Unit,
    private val saveAction: () -> Unit,
    private val saveAsAction: () -> Unit,
    private val undoAction: () -> Unit,
    private val redoAction: () -> Unit,
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

    private data class ToolbarEntry(val id: String, val window: CollapsibleWindow)
    private data class BuiltToolbar(val window: CollapsibleWindow, val content: VisTable, val actors: List<Actor>)

    private inner open class CollapsibleWindow(
        title: String,
        private val showCloseButton: Boolean = false
    ) : VisWindow(title, true) {
        private val baseTitle = title
        private val compactTitle = title.take(4)
        private var collapsed = false

        init {
            isMovable = true
            isResizable = false
            isModal = false
            setKeepWithinParent(false)
            if (showCloseButton) {
                addCloseButton()
            }
            getTitleTable().addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (tapCount >= 2) {
                        toggleCollapsed()
                    }
                }
            })
        }

        override fun close() {
            isVisible = false
        }

        fun setCompactTitle(compact: Boolean) {
            getTitleLabel().setText(if (compact) compactTitle else baseTitle)
        }

        fun setCollapsedState(value: Boolean) {
            if (collapsed != value) {
                toggleCollapsed()
            }
        }

        private fun toggleCollapsed() {
            val oldTop = y + height
            collapsed = !collapsed
            val titleTable = getTitleTable()
            children.forEach { child ->
                if (child !== titleTable) {
                    child.isVisible = !collapsed
                }
            }
            invalidateHierarchy()
            pack()
            setY(oldTop - height)
            layoutDockPanel()
        }
    }

    private inner class DockSection(
        private val titleText: String,
        private val body: Actor
    ) : WidgetGroup() {
        private val header = VisTable()
        private val titleLabel = VisLabel()
        private var collapsed = false
        private val headerHeight = 24f
        private val padding = 4f

        init {
            touchable = Touchable.enabled
            header.add(titleLabel).left().padLeft(6f).growX()
            addActor(header)
            addActor(body)
            updateHeader()
            header.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    toggleCollapsed()
                }
            })
        }

        fun setCollapsedState(value: Boolean) {
            if (collapsed != value) {
                toggleCollapsed()
            }
        }

        private fun toggleCollapsed() {
            collapsed = !collapsed
            body.isVisible = !collapsed
            updateHeader()
            invalidateHierarchy()
            layoutDockPanel()
        }

        private fun updateHeader() {
            header.background = if (collapsed) dockClosedDrawable() else dockOpenDrawable()
            titleLabel.setText((if (collapsed) "▶ " else "▼ ") + titleText)
            titleLabel.color = Color.WHITE
        }

        override fun getPrefWidth(): Float {
            if (!isVisible) {
                return 0f
            }
            val bodyLayout = body as? Layout
            return max(header.prefWidth, (bodyLayout?.prefWidth ?: body.width) + padding * 2f)
        }

        override fun getPrefHeight(): Float {
            if (!isVisible) {
                return 0f
            }
            val bodyLayout = body as? Layout
            val bodyHeight = if (body.isVisible) bodyLayout?.prefHeight ?: body.height else 0f
            return headerHeight + if (body.isVisible) padding * 2f + bodyHeight else 0f
        }

        override fun layout() {
            val w = width.coerceAtLeast(1f)
            header.setBounds(0f, height - headerHeight, w, headerHeight)
            if (body.isVisible) {
                body.setBounds(
                    padding,
                    padding,
                    (w - padding * 2f).coerceAtLeast(1f),
                    (height - headerHeight - padding * 2f).coerceAtLeast(1f)
                )
                (body as? Layout)?.let {
                    it.invalidate()
                    it.validate()
                }
            }
        }
    }

    private val uiViewport = ScreenViewport()
    val stage: Stage = Stage(uiViewport)
    private val prefs = Gdx.app.getPreferences("voxcraft-ui")
    private val whiteTexture = createWhiteTexture()
    private val ownedTextures = mutableListOf<Texture>()
    private val toolbarEntries = linkedMapOf<String, CollapsibleWindow>()
    private val toolbarContents = linkedMapOf<String, VisTable>()
    private val toolbarActors = linkedMapOf<String, MutableList<Actor>>()
    private val toolbarRenderedActors = linkedMapOf<String, List<Actor>>()
    private val toolGroup = ButtonGroup<AppImageTextButton>()
    private val toolButtons = linkedMapOf<Int, AppImageTextButton>()
    private val buttonLabels = mutableMapOf<AppImageTextButton, String>()
    private val buttonMarkers = mutableMapOf<AppImageTextButton, Actor>()
    private val cameraButtons = linkedMapOf<CameraMode, AppImageTextButton>()
    private val cameraInteractionButtons = linkedMapOf<CameraInteractionMode, AppImageTextButton>()
    private val addModeButtons = linkedMapOf<String, AppImageTextButton>()
    private val iconDrawables = mutableMapOf<String, TextureRegionDrawable>()
    private val currentColorPreview = ColorChip(whiteTexture) { currentColorProvider() }
    private val cubeToolbarLabel = VisLabel("")
    private val statusBar = VisTable()
    private val statusLine = VisLabel("")
    private val rightDockWindow = CollapsibleWindow("Panels")
    private val rightDockContent = VisTable()
    private val modelGridField = VisTextField("")
    private val modelUnitSizeField = VisTextField("")
    private val modelUnitSuffixField = VisTextField("")
    private val uiScaleSelect = VisSelectBox<String>()
    private val toolbarSizeSelect = VisSelectBox<String>()
    private val toolbarAutoCollapseCheck = VisCheckBox("Auto-collapse toolbars")
    private lateinit var inToolOperatorsContent: VisTable
    private var colorPicker: ColorPicker? = null
    private var exportDialog: VisWindow? = null
    private var hoverPopover: VisWindow? = null
    private var toolOperatorsSignature = ""
    private var syncing = false
    private var toolbarsPositioned = false
    private var expandedToolbarId: String? = null
    private var toolbarIconSizePx = prefs.getInteger("toolbar.iconSize", 32).coerceToToolbarIconSize()
    private var toolbarButtonSize = toolbarIconSizePx.toFloat()
    private var toolbarAutoCollapse = prefs.getBoolean("toolbar.autoCollapse", false)

    init {
        setUiScale(uiScaleProvider())
        loadIconDrawables()
        toolGroup.setMinCheckCount(1)
        toolGroup.setMaxCheckCount(1)
        toolGroup.setUncheckLast(false)

        statusBar.background = darkBarDrawable()
        statusBar.defaults().pad(2f)
        statusLine.setWrap(false)
        statusBar.add(statusLine).left()
        stage.addActor(statusBar)

        buildToolbars()
        buildRightDock()
        layoutStaticPanels()
        arrangeToolbarsHorizontalFlow()
        syncFromState()
    }

    fun act(delta: Float) {
        syncFromState()
        stage.act(delta)
        updateToolbarHoverExpansion()
    }

    fun draw() {
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        uiViewport.update(width, height, true)
        layoutStaticPanels()
        clampToolbarsToViewport()
    }

    fun setUiScale(scale: Float) {
        val normalized = normalizeUiScale(scale)
        uiViewport.setUnitsPerPixel(1f / normalized)
        uiViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        prefs.putFloat("ui.scale", normalized)
        prefs.flush()
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
        ownedTextures.forEach { it.dispose() }
        stage.dispose()
    }

    private fun buildToolbars() {
        toolbarEntries.values.forEach { it.remove() }
        toolbarEntries.clear()
        toolbarContents.clear()
        toolbarActors.clear()
        toolbarRenderedActors.clear()
        toolButtons.clear()
        cameraButtons.clear()
        cameraInteractionButtons.clear()
        addModeButtons.clear()
        buttonLabels.clear()
        buttonMarkers.clear()
        toolGroup.buttons.clear()

        val functions = buildFunctionsToolbar()
        registerToolbar("functions", functions)
        val modification = buildToolsToolbar("Modification", true)
        registerToolbar("modification", modification)
        val construction = buildToolsToolbar("Construction", false)
        registerToolbar("construction", construction)
        val camera = buildCameraToolbar()
        registerToolbar("camera", camera)
        val cubes = buildCubesToolbar()
        registerToolbar("cubes", cubes)
        val inTool = buildToolOperatorsToolbar()
        registerToolbar("in_tool", inTool)
        toolbarsPositioned = false
    }

    private fun registerToolbar(id: String, toolbar: BuiltToolbar) {
        val window = toolbar.window
        toolbarEntries[id] = window
        toolbarContents[id] = toolbar.content
        toolbarActors[id] = toolbar.actors.toMutableList()
        renderToolbarActors(id, toolbar.actors)
        window.addListener(object : ClickListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (pointer == -1 && toolbarAutoCollapse) {
                    expandedToolbarId = id
                    updateAutoCollapsedToolbars()
                }
            }
        })
        stage.addActor(window)
    }

    private fun renderToolbarActors(id: String, actors: List<Actor>) {
        val content = toolbarContents[id] ?: return
        content.clearChildren()
        actors.forEach { actor ->
            actor.isVisible = true
            addToolbarActor(id, content, actor)
        }
        toolbarRenderedActors[id] = actors.toList()
        content.invalidateHierarchy()
    }

    private fun addToolbarActor(id: String, content: VisTable, actor: Actor) {
        val pad = if (id == "cubes") 2f else 0f
        when (actor) {
            is AppImageTextButton,
            is ColorChip -> content.add(actor).size(toolbarButtonSize, toolbarButtonSize).pad(pad)

            is VisTextButton -> {
                val minWidth = actor.prefWidth.coerceAtLeast(if (actor.isDisabled) 86f else 58f)
                content.add(actor).height(toolbarButtonSize).minWidth(minWidth).pad(0f)
            }

            is VisLabel -> content.add(actor).left().pad(pad)

            else -> content.add(actor).pad(pad)
        }
    }

    private fun buildFunctionsToolbar(): BuiltToolbar {
        val window = CollapsibleWindow("Functions")
        val content = VisTable()
        content.defaults().pad(0f)
        val actors = listOf(
            imageAction("Open", "file_open", openAction),
            imageAction("Import VXDI", "file_open", importAction),
            imageAction("Save", "file_save", saveAction),
            imageAction("Save As", "file_save", saveAsAction),
            imageAction("Export", "file_save") { showExportDialog() },
            imageAction("Undo", "undo", undoAction),
            imageAction("Redo", "redo", redoAction),
            imageAction("Reset Tool", "reset", resetToolAction),
            imageAction("Delete Selection", "delete", deleteSelectionAction),
            imageAction("Clear Selection", "clear_selection", clearSelectionAction),
            imageAction("Clear Guides", "clear_guides", clearGuidesAction)
        )
        window.add(content).pad(0f).left()
        window.pack()
        return BuiltToolbar(window, content, actors)
    }

    private fun buildToolsToolbar(title: String, modification: Boolean): BuiltToolbar {
        val window = CollapsibleWindow(title)
        val content = VisTable()
        content.defaults().pad(0f)
        val actors = mutableListOf<Actor>()
        toolNamesProvider().forEachIndexed { index, toolName ->
            if (isModificationTool(toolName) != modification) {
                return@forEachIndexed
            }
            val icon = iconNameForTool(toolName)
            val button = imageAction(toolName, icon) { toolSelected(index) }
            button.isChecked = activeToolIndexProvider() == index
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (!syncing && button.isChecked) {
                        toolSelected(index)
                    }
                }
            })
            toolGroup.add(button)
            toolButtons[index] = button
            actors += button
        }
        window.add(content).pad(0f).left()
        window.pack()
        return BuiltToolbar(window, content, actors)
    }

    private fun buildCameraToolbar(): BuiltToolbar {
        val window = CollapsibleWindow("Camera")
        val content = VisTable()
        content.defaults().pad(0f)
        val actors = mutableListOf<Actor>()
        val cameraGroup = ButtonGroup<AppImageTextButton>().apply {
            setMinCheckCount(1)
            setMaxCheckCount(1)
            setUncheckLast(false)
        }
        CameraMode.entries.forEach { mode ->
            val button = imageAction(mode.displayName, iconNameForCameraMode(mode)) { cameraModeChanged(mode) }
            cameraGroup.add(button)
            cameraButtons[mode] = button
            actors += button
        }
        val dragGroup = ButtonGroup<AppImageTextButton>().apply {
            setMinCheckCount(1)
            setMaxCheckCount(1)
            setUncheckLast(false)
        }
        CameraInteractionMode.entries.forEach { mode ->
            val button = imageAction("Drag ${mode.displayName}", iconNameForCameraInteractionMode(mode)) {
                cameraInteractionModeChanged(mode)
            }
            dragGroup.add(button)
            cameraInteractionButtons[mode] = button
            actors += button
        }
        listOf(
            OrthographicView.TOP,
            OrthographicView.FRONT,
            OrthographicView.LEFT,
            OrthographicView.RIGHT,
            OrthographicView.BACK
        ).forEach { view ->
            actors += imageAction("View ${view.displayName}", iconNameForView(view)) {
                orthographicViewChanged(view)
            }
        }
        window.add(content).pad(0f).left()
        window.pack()
        return BuiltToolbar(window, content, actors)
    }

    private fun buildCubesToolbar(): BuiltToolbar {
        val window = CollapsibleWindow("Cubes")
        val content = VisTable()
        content.defaults().pad(2f)
        currentColorPreview.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showColorPicker()
            }
        })
        val actors = mutableListOf<Actor>(cubeToolbarLabel, currentColorPreview)
        val addGroup = ButtonGroup<AppImageTextButton>().apply {
            setMinCheckCount(1)
            setMaxCheckCount(1)
            setUncheckLast(false)
        }
        listOf(
            "addWithoutReplace" to ("Add without replace" to "add_without_replace"),
            "addOrReplace" to ("Add or replace" to "add_or_replace"),
            "replaceCube" to ("Replace existing cube" to "replace_only")
        ).forEach { (mode, labelIcon) ->
            val button = imageAction(labelIcon.first, labelIcon.second) { addModeChanged(mode) }
            addGroup.add(button)
            addModeButtons[mode] = button
            actors += button
        }
        window.add(content).pad(2f).left()
        window.pack()
        return BuiltToolbar(window, content, actors)
    }

    private fun buildToolOperatorsToolbar(): BuiltToolbar {
        val window = CollapsibleWindow("In-Tool Operators")
        inToolOperatorsContent = VisTable()
        inToolOperatorsContent.defaults().pad(0f)
        window.add(inToolOperatorsContent).pad(0f).left()
        window.pack()
        return BuiltToolbar(window, inToolOperatorsContent, emptyList())
    }

    private fun buildRightDock() {
        val scroll = VisScrollPane(rightDockContent)
        scroll.setFadeScrollBars(false)
        scroll.setScrollingDisabled(true, false)
        rightDockWindow.isMovable = false
        rightDockWindow.isResizable = false
        rightDockWindow.add(scroll).grow().pad(4f)
        rightDockContent.top().left()
        rightDockContent.defaults().growX().padBottom(4f)
        rightDockContent.add(buildModelSettingsPanel()).growX().row()
        rightDockContent.add(buildUiSettingsPanel()).growX().row()
        stage.addActor(rightDockWindow)
    }

    private fun buildModelSettingsPanel(): DockSection {
        val content = VisTable()
        content.background = darkPanelDrawable()
        content.defaults().pad(4f).left().growX()
        content.add(VisLabel("Grid size")).left().row()
        content.add(modelGridField).growX().row()
        content.add(VisLabel("Unit size")).left().row()
        content.add(modelUnitSizeField).growX().row()
        content.add(VisLabel("Unit suffix")).left().row()
        content.add(modelUnitSuffixField).growX().row()

        listOf(modelGridField, modelUnitSizeField, modelUnitSuffixField).forEach { field ->
            field.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (!syncing) {
                        applyModelSettingsFields()
                    }
                }
            })
        }
        return DockSection("Model Settings", content).apply { setCollapsedState(false) }
    }

    private fun buildUiSettingsPanel(): DockSection {
        val content = VisTable()
        content.background = darkPanelDrawable()
        content.defaults().pad(4f).left().growX()
        uiScaleSelect.setItems("1x", "1.5x", "2x")
        toolbarSizeSelect.setItems("32 x 32 px", "48 x 48 px", "64 x 64 px")
        toolbarAutoCollapseCheck.isChecked = toolbarAutoCollapse
        content.add(VisLabel("UI text size")).left().row()
        content.add(uiScaleSelect).growX().row()
        content.add(toolbarAutoCollapseCheck).left().row()
        content.add(VisLabel("Toolbar icon/button size")).left().row()
        content.add(toolbarSizeSelect).growX().row()
        content.add(VisLabel("Arrange toolbars")).left().padTop(6f).row()
        val arrangeRow = VisTable()
        arrangeRow.defaults().pad(2f)
        val horizontal = VisTextButton("Flow L->R")
        val vertical = VisTextButton("Flow T->D")
        arrangeRow.add(horizontal).growX()
        arrangeRow.add(vertical).growX()
        content.add(arrangeRow).growX().row()
        val note = VisLabel("Affects built-in and mapped toolbar icons.")
        note.setWrap(true)
        content.add(note).width(250f).left().row()

        uiScaleSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (syncing) return
                val scale = parseUiScaleLabel(uiScaleSelect.selected)
                uiScaleChanged(scale)
                setUiScale(scale)
                layoutStaticPanels()
            }
        })
        toolbarSizeSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (!syncing) {
                    setToolbarIconSize(parseToolbarSize(toolbarSizeSelect.selected))
                }
            }
        })
        toolbarAutoCollapseCheck.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (!syncing) {
                    toolbarAutoCollapse = toolbarAutoCollapseCheck.isChecked
                    prefs.putBoolean("toolbar.autoCollapse", toolbarAutoCollapse)
                    prefs.flush()
                    applyToolbarCompactState()
                }
            }
        })
        horizontal.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                arrangeToolbarsHorizontalFlow()
            }
        })
        vertical.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                arrangeToolbarsVerticalFlow()
            }
        })

        return DockSection("UI Settings", content).apply { setCollapsedState(false) }
    }

    private fun imageAction(label: String, iconName: String, action: () -> Unit): AppImageTextButton {
        val icon = iconFor(iconName, createActionIconDrawable(Color(0.35f, 0.35f, 0.35f, 1f)))
        val button = AppImageTextButton("", icon)
        applyButtonStyle(button, icon)
        buttonLabels[button] = label
        attachButtonMarker(button)
        attachPopover(button, label)
        button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                action()
            }
        })
        return button
    }

    private fun showExportDialog() {
        val choices = exportChoicesProvider()
        exportDialog?.remove()
        val dialog = CollapsibleWindow("Export", showCloseButton = true).also {
            it.isModal = true
            exportDialog = it
        }
        val content = VisTable(true)
        if (choices.isEmpty()) {
            content.add(VisLabel("No export formats are available.")).pad(10f).row()
            val close = VisTextButton("Close")
            close.addListener(onChange { dialog.remove() })
            content.add(close).right().pad(10f)
        } else {
            val select = VisSelectBox<String>()
            select.setItems(*choices.toTypedArray())
            content.add(VisLabel("Format")).left().padRight(8f)
            content.add(select).width(280f).left().row()
            val export = VisTextButton("Export")
            val cancel = VisTextButton("Cancel")
            export.addListener(onChange {
                val selected = select.selected
                dialog.remove()
                exportChoiceSelected(selected)
            })
            cancel.addListener(onChange { dialog.remove() })
            content.add(export).pad(8f)
            content.add(cancel).pad(8f)
        }
        dialog.add(content).pad(8f)
        stage.addActor(dialog)
        dialog.pack()
        dialog.centerWindow()
        dialog.toFront()
    }

    private fun rebuildToolOperatorsIfNeeded(toolName: String) {
        if (!::inToolOperatorsContent.isInitialized) {
            return
        }
        val operators = toolOperatorsProvider()
        val signature = buildString {
            append(toolName)
            operators.forEach { operator ->
                append('|').append(operator.label).append(':').append(operator.active()).append(':').append(operator.enabled())
            }
        }
        if (signature == toolOperatorsSignature) {
            return
        }
        toolOperatorsSignature = signature
        val actors = mutableListOf<Actor>()
        val toolNameButton = VisTextButton(toolName.ifBlank { "-" }, "toggle")
        toolNameButton.isDisabled = true
        actors += toolNameButton
        operators.forEach { operator ->
            val label = if (operator.active()) "* ${operator.label}" else operator.label
            val button = VisTextButton(label)
            button.isDisabled = !operator.enabled()
            button.addListener(onChange {
                if (operator.enabled()) {
                    operator.action()
                    toolOperatorsSignature = ""
                }
            })
            actors += button
        }
        toolbarActors["in_tool"] = actors
        toolbarRenderedActors.remove("in_tool")
        updateAutoCollapsedToolbars()
    }

    private fun syncFromState() {
        syncing = true
        val snapshot = statusProvider()
        toolButtons.forEach { (index, button) ->
            button.isChecked = activeToolIndexProvider() == index
        }
        cameraButtons.forEach { (mode, button) ->
            button.isChecked = cameraModeProvider() == mode
        }
        cameraInteractionButtons.forEach { (mode, button) ->
            button.isChecked = cameraInteractionModeProvider() == mode
        }
        addModeButtons.forEach { (mode, button) ->
            button.isChecked = addModeProvider() == mode
        }
        cubeToolbarLabel.setText(if (snapshot.selectionCount > 0) "Selection" else "Current")
        uiScaleSelect.selected = uiScaleLabel(uiScaleProvider())
        toolbarSizeSelect.selected = toolbarSizeLabel(toolbarIconSizePx)
        toolbarAutoCollapseCheck.isChecked = toolbarAutoCollapse
        syncModelSettingsFields()
        statusLine.setText(
            "File: ${snapshot.fileName} | Camera: ${snapshot.cameraMode} | Add: ${snapshot.addMode} | " +
                "Tool: ${snapshot.activeTool} | Cubes: ${snapshot.cubeCount} | Selection: ${snapshot.selectionCount} | " +
                "Guides: ${snapshot.guideCount} | ${snapshot.message} | ${snapshot.cursor}"
        )
        updateButtonMarkers()
        rebuildToolOperatorsIfNeeded(snapshot.activeTool)
        if (!toolbarsPositioned) {
            arrangeToolbarsHorizontalFlow()
        }
        syncing = false
    }

    private fun syncModelSettingsFields() {
        val settings = modelSettingsProvider()
        setFieldTextIfNotFocused(modelGridField, settings.gridSize.toString())
        setFieldTextIfNotFocused(modelUnitSizeField, settings.unitSize.toString())
        setFieldTextIfNotFocused(modelUnitSuffixField, settings.unitSuffix)
    }

    private fun setFieldTextIfNotFocused(field: VisTextField, value: String) {
        if (stage.keyboardFocus !== field && field.text != value) {
            field.text = value
        }
    }

    private fun applyModelSettingsFields() {
        val current = modelSettingsProvider()
        val next = ModelSettings(
            gridSize = modelGridField.text.toIntOrNull()?.coerceAtLeast(1) ?: current.gridSize,
            unitSize = modelUnitSizeField.text.toFloatOrNull()?.coerceAtLeast(1e-6f) ?: current.unitSize,
            unitSuffix = modelUnitSuffixField.text.ifBlank { current.unitSuffix }
        )
        modelSettingsChanged(next)
    }

    private fun layoutStaticPanels() {
        val width = viewportWidth()
        val height = viewportHeight()
        val statusHeight = 24f
        statusBar.setBounds(0f, 0f, width, statusHeight)
        statusLine.setBounds(4f, 0f, width * 2f, statusHeight)

        rightDockWindow.pack()
        val dockWidth = 300f.coerceAtMost((width * 0.42f).coerceAtLeast(220f))
        val dockHeight = (height - statusHeight - 16f).coerceAtLeast(120f)
        rightDockWindow.setSize(dockWidth, dockHeight)
        rightDockWindow.setPosition(width - dockWidth - 8f, statusHeight + 8f)
        layoutDockPanel()
    }

    private fun layoutDockPanel() {
        rightDockContent.invalidateHierarchy()
        rightDockWindow.invalidateHierarchy()
    }

    private fun arrangeToolbarsHorizontalFlow() {
        val width = viewportWidth()
        val height = viewportHeight()
        val margin = 4f
        val gap = 4f
        val rightLimit = if (rightDockWindow.isVisible) rightDockWindow.x - gap else width - margin
        var x = margin
        var yTop = height - margin
        var rowHeight = 0f
        orderedToolbarEntries().forEach { entry ->
            val window = entry.window
            window.invalidateHierarchy()
            window.pack()
            if (x + window.width > rightLimit && x > margin) {
                x = margin
                yTop -= rowHeight + gap
                rowHeight = 0f
            }
            window.setPosition(x, yTop - window.height)
            window.toFront()
            x += window.width + gap
            rowHeight = max(rowHeight, window.height)
        }
        rightDockWindow.toFront()
        statusBar.toFront()
        toolbarsPositioned = true
    }

    private fun arrangeToolbarsVerticalFlow() {
        val height = viewportHeight()
        val margin = 4f
        val gap = 4f
        var x = margin
        var yTop = height - margin
        var columnWidth = 0f
        orderedToolbarEntries().forEach { entry ->
            val window = entry.window
            window.invalidateHierarchy()
            window.pack()
            if (yTop < height - margin && yTop - window.height < 32f) {
                x += columnWidth + gap
                yTop = height - margin
                columnWidth = 0f
            }
            window.setPosition(x, yTop - window.height)
            window.toFront()
            yTop -= window.height + gap
            columnWidth = max(columnWidth, window.width)
        }
        rightDockWindow.toFront()
        statusBar.toFront()
        toolbarsPositioned = true
    }

    private fun orderedToolbarEntries(): List<ToolbarEntry> {
        return listOf("functions", "modification", "construction", "camera", "cubes", "in_tool")
            .mapNotNull { id -> toolbarEntries[id]?.let { ToolbarEntry(id, it) } }
    }

    private fun clampToolbarsToViewport() {
        val width = viewportWidth()
        val height = viewportHeight()
        toolbarEntries.values.forEach { window ->
            val x = window.x.coerceIn(0f, (width - window.width).coerceAtLeast(0f))
            val y = window.y.coerceIn(24f, (height - window.height).coerceAtLeast(24f))
            window.setPosition(x, y)
        }
    }

    private fun setToolbarIconSize(size: Int) {
        val normalized = size.coerceToToolbarIconSize()
        if (normalized == toolbarIconSizePx) {
            return
        }
        toolbarIconSizePx = normalized
        toolbarButtonSize = normalized.toFloat()
        prefs.putInteger("toolbar.iconSize", normalized)
        prefs.flush()
        iconDrawables.clear()
        loadIconDrawables()
        buildToolbars()
        layoutStaticPanels()
        arrangeToolbarsHorizontalFlow()
    }

    private fun applyToolbarCompactState() {
        toolbarEntries.values.forEach { it.setCompactTitle(toolbarAutoCollapse) }
        if (!toolbarAutoCollapse) {
            expandedToolbarId = null
        }
        updateAutoCollapsedToolbars()
    }

    private fun updateToolbarHoverExpansion() {
        if (!toolbarAutoCollapse) {
            return
        }
        val pointer = stage.screenToStageCoordinates(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
        val hoveredId = toolbarEntries.entries.firstOrNull { (_, window) ->
            window.isVisible &&
                pointer.x >= window.x &&
                pointer.x <= window.x + window.width &&
                pointer.y >= window.y &&
                pointer.y <= window.y + window.height
        }?.key
        if (hoveredId != expandedToolbarId) {
            expandedToolbarId = hoveredId
            updateAutoCollapsedToolbars()
        }
    }

    private fun updateAutoCollapsedToolbars() {
        toolbarEntries.forEach { (id, window) ->
            val actors = toolbarActors[id] ?: return@forEach
            val representative = representativeActorForToolbar(id, actors)
            val collapsed = toolbarAutoCollapse && expandedToolbarId != id && representative != null
            val desiredActors = if (collapsed) listOf(representative!!) else actors
            if (!sameActorList(toolbarRenderedActors[id], desiredActors)) {
                val oldTop = window.y + window.height
                renderToolbarActors(id, desiredActors)
                window.invalidateHierarchy()
                window.pack()
                window.setY(oldTop - window.height)
            }
        }
    }

    private fun sameActorList(left: List<Actor>?, right: List<Actor>): Boolean {
        return left != null && left.size == right.size && left.indices.all { left[it] === right[it] }
    }

    private fun representativeActorForToolbar(id: String, actors: List<Actor>): Actor? {
        fun contains(actor: Actor): Boolean = actors.any { it === actor }
        return when (id) {
            "modification", "construction" ->
                toolButtons.entries.firstOrNull { (_, button) -> contains(button) && button.isChecked }?.value
                    ?: actors.firstOrNull { it is AppImageTextButton }

            "camera" ->
                cameraButtons.entries.firstOrNull { (_, button) -> contains(button) && button.isChecked }?.value
                    ?: cameraInteractionButtons.entries.firstOrNull { (_, button) -> contains(button) && button.isChecked }?.value
                    ?: actors.firstOrNull { it is AppImageTextButton }

            "cubes" ->
                addModeButtons.entries.firstOrNull { (_, button) -> contains(button) && button.isChecked }?.value
                    ?: actors.firstOrNull { it is AppImageTextButton }
                    ?: currentColorPreview

            else -> actors.firstOrNull { it is AppImageTextButton } ?: actors.firstOrNull()
        }
    }

    private fun updateButtonMarkers() {
        val activeIndex = activeToolIndexProvider()
        toolButtons.forEach { (index, button) ->
            buttonMarkers[button]?.color = if (index == activeIndex) Color(0.9f, 0.1f, 0.1f, 1f) else Color.WHITE
        }
        cameraButtons.forEach { (mode, button) ->
            buttonMarkers[button]?.color = if (mode == cameraModeProvider()) Color(0.9f, 0.1f, 0.1f, 1f) else Color.WHITE
        }
        cameraInteractionButtons.forEach { (mode, button) ->
            buttonMarkers[button]?.color = if (mode == cameraInteractionModeProvider()) Color(0.9f, 0.1f, 0.1f, 1f) else Color.WHITE
        }
        addModeButtons.forEach { (mode, button) ->
            buttonMarkers[button]?.color = if (mode == addModeProvider()) Color(0.9f, 0.1f, 0.1f, 1f) else Color.WHITE
        }
        updateAutoCollapsedToolbars()
    }

    private fun attachButtonMarker(button: AppImageTextButton) {
        val marker = ColorChip(whiteTexture) { Color.WHITE }.apply {
            setSize((toolbarButtonSize * 0.22f).coerceIn(7f, 14f), (toolbarButtonSize * 0.22f).coerceIn(7f, 14f))
            touchable = Touchable.disabled
        }
        button.addActor(marker)
        buttonMarkers[button] = marker
    }

    private fun attachPopover(button: AppImageTextButton, label: String) {
        button.addListener(object : ClickListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (pointer == -1) {
                    showPopover(button, label)
                }
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                if (pointer == -1) {
                    hidePopover()
                }
            }

            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, buttonCode: Int): Boolean {
                hidePopover()
                return false
            }
        })
    }

    private fun showPopover(anchor: Actor, text: String) {
        hidePopover()
        val window = CollapsibleWindow(text).also { hoverPopover = it }
        window.isMovable = false
        window.add(VisLabel(text)).pad(6f)
        stage.addActor(window)
        window.pack()
        val pos = anchor.localToStageCoordinates(Vector2(0f, 0f))
        val x = pos.x.coerceIn(0f, (viewportWidth() - window.width).coerceAtLeast(0f))
        val y = (pos.y - window.height - 4f).coerceAtLeast(24f)
        window.setPosition(x, y)
        window.toFront()
    }

    private fun hidePopover() {
        hoverPopover?.remove()
        hoverPopover = null
    }

    private fun showColorPicker() {
        if (colorPicker == null) {
            colorPicker = ColorPicker("Color").apply {
                setListener(object : ColorPickerListener {
                    override fun changed(color: Color?) {
                        color?.let { colorSelected(Color(it)) }
                    }

                    override fun canceled(oldColor: Color?) {
                    }

                    override fun reset(oldColor: Color?, newColor: Color?) {
                        newColor?.let { colorSelected(Color(it)) }
                    }

                    override fun finished(color: Color?) {
                        color?.let { colorSelected(Color(it)) }
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
        picker.toFront()
    }

    private fun applyButtonStyle(button: AppImageTextButton, icon: Drawable) {
        val style = button.style
        val background = buttonUpDrawable()
        style.up = background
        style.down = background
        style.checked = background
        style.over = background
        style.imageUp = icon
        style.imageDown = icon
        style.imageChecked = icon
        style.imageOver = icon
        style.fontColor = Color.BLACK
        style.checkedFontColor = Color.BLACK
        style.overFontColor = Color.BLACK
        button.style = style
        button.setText("")
        button.image?.drawable = icon
        button.imageCell?.size((toolbarButtonSize - 4f).coerceAtLeast(12f))
    }

    private fun loadIconDrawables() {
        val mappingFile = Gdx.files.internal("icons.mapping.csv")
        val textureFile = when (toolbarIconSizePx) {
            48 -> listOf("icons-48px.png", "icons.48.png")
            64 -> listOf("icons-64px.png", "icons.64.png")
            else -> listOf("icons-32px.png", "icons.png")
        }.map { Gdx.files.internal(it) }.firstOrNull { it.exists() } ?: return
        if (!mappingFile.exists()) {
            return
        }
        val texture = Texture(textureFile)
        ownedTextures += texture
        val coordStartIndex = when (toolbarIconSizePx) {
            48 -> 8
            64 -> 12
            else -> 4
        }
        mappingFile.readString("UTF-8").lineSequence().drop(1).forEach { line ->
            val parts = line.split('|')
            if (parts.size < coordStartIndex + 4) {
                return@forEach
            }
            val name = parts[1].trim()
            if (name.isBlank() || name.startsWith("Undefined")) {
                return@forEach
            }
            val startX = parts[coordStartIndex].toIntOrNull() ?: return@forEach
            val endX = parts[coordStartIndex + 1].toIntOrNull() ?: return@forEach
            val startY = parts[coordStartIndex + 2].toIntOrNull() ?: return@forEach
            val endY = parts[coordStartIndex + 3].toIntOrNull() ?: return@forEach
            iconDrawables.putIfAbsent(name, TextureRegionDrawable(TextureRegion(texture, startX, startY, endX - startX + 1, endY - startY + 1)))
        }
    }

    private fun iconFor(name: String, fallback: Drawable): TextureRegionDrawable {
        return iconDrawables[name] ?: (fallback as? TextureRegionDrawable) ?: createActionIconDrawable(Color.DARK_GRAY)
    }

    private fun iconNameForTool(toolName: String): String {
        return when (toolName.lowercase()) {
            "select2", "select" -> "select"
            "move" -> "move"
            "copy" -> "copy"
            "rotate" -> "rotate"
            "copyrot" -> "rotate_copy"
            "voxel" -> "voxel"
            "segment" -> "segment"
            "polyline" -> "polyline"
            "arc" -> "arc"
            "circle" -> "circle"
            "plane" -> "plane"
            "sphere" -> "sphere"
            "cloud" -> "fuzzy_sphere"
            "ball" -> "hollow_sphere"
            "frame" -> "frame"
            "shell" -> "shell"
            "volume" -> "volume"
            "axial grid" -> "axial_grid_helper"
            "planar grid" -> "planar_grid_helper"
            "import vxdi" -> "file_open"
            else -> "select"
        }
    }

    private fun iconNameForCameraMode(mode: CameraMode): String {
        return when (mode) {
            CameraMode.ORBIT -> "camera_perspective"
            CameraMode.WALKTHROUGH -> "camera_walkthrough"
            CameraMode.ORTHOGRAPHIC -> "camera_orthogonal"
        }
    }

    private fun iconNameForCameraInteractionMode(mode: CameraInteractionMode): String {
        return when (mode) {
            CameraInteractionMode.TOOL -> "select"
            CameraInteractionMode.ROTATE -> "camera_rotate"
            CameraInteractionMode.PAN -> "camera_pan"
            CameraInteractionMode.ZOOM -> "camera_zoom"
        }
    }

    private fun iconNameForView(view: OrthographicView): String {
        return when (view) {
            OrthographicView.TOP, OrthographicView.BOTTOM -> "view_top"
            OrthographicView.FRONT -> "view_front"
            OrthographicView.LEFT -> "view_left"
            OrthographicView.RIGHT -> "view_right"
            OrthographicView.BACK -> "view_back"
        }
    }

    private fun isModificationTool(toolName: String): Boolean {
        return toolName.lowercase() in setOf("select2", "select", "move", "copy", "rotate", "copyrot")
    }

    private fun createActionIconDrawable(color: Color): TextureRegionDrawable {
        val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fillRectangle(5, 5, 22, 22)
        pixmap.setColor(Color.WHITE)
        pixmap.drawRectangle(5, 5, 22, 22)
        val texture = Texture(pixmap)
        pixmap.dispose()
        ownedTextures += texture
        return TextureRegionDrawable(TextureRegion(texture))
    }

    private fun buttonUpDrawable(): TextureRegionDrawable = createSolidBorderDrawable(Color.WHITE, Color(0.55f, 0.55f, 0.55f, 1f))

    private fun darkBarDrawable(): TextureRegionDrawable = createSolidBorderDrawable(Color.valueOf("555555"), Color.valueOf("555555"))

    private fun darkPanelDrawable(): TextureRegionDrawable = createSolidBorderDrawable(Color.valueOf("555555"), Color.valueOf("555555"))

    private fun dockOpenDrawable(): TextureRegionDrawable = createSolidBorderDrawable(Color.valueOf("168ccc"), Color.valueOf("168ccc"))

    private fun dockClosedDrawable(): TextureRegionDrawable = createSolidBorderDrawable(Color.valueOf("3c4650"), Color.valueOf("657380"))

    private fun createSolidBorderDrawable(fill: Color, border: Color): TextureRegionDrawable {
        val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
        pixmap.setColor(fill)
        pixmap.fill()
        pixmap.setColor(border)
        pixmap.drawRectangle(0, 0, 16, 16)
        val texture = Texture(pixmap)
        pixmap.dispose()
        ownedTextures += texture
        return TextureRegionDrawable(TextureRegion(texture))
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

    private fun viewportWidth(): Float = if (stage.viewport.screenWidth > 0) stage.viewport.screenWidth.toFloat() else Gdx.graphics.width.toFloat()

    private fun viewportHeight(): Float = if (stage.viewport.screenHeight > 0) stage.viewport.screenHeight.toFloat() else Gdx.graphics.height.toFloat()

    private fun normalizeUiScale(scale: Float): Float {
        return when {
            scale >= 1.75f -> 2f
            scale >= 1.25f -> 1.5f
            else -> 1f
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

    private fun parseToolbarSize(label: String?): Int {
        return when (label) {
            "48 x 48 px" -> 48
            "64 x 64 px" -> 64
            else -> 32
        }
    }

    private fun toolbarSizeLabel(size: Int): String {
        return when (size.coerceToToolbarIconSize()) {
            48 -> "48 x 48 px"
            64 -> "64 x 64 px"
            else -> "32 x 32 px"
        }
    }

    private fun Int.coerceToToolbarIconSize(): Int {
        return when {
            this >= 56 -> 64
            this >= 40 -> 48
            else -> 32
        }
    }

    private class ColorChip(
        private val texture: Texture,
        val colorProvider: () -> Color
    ) : Actor() {
        init {
            setSize(28f, 28f)
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            val base = Color(colorProvider()).mul(color)
            val original = batch.color.cpy()
            batch.color = Color(base.r, base.g, base.b, base.a * parentAlpha)
            batch.draw(texture, x, y, width, height)
            batch.color = Color(0f, 0f, 0f, 0.55f * parentAlpha)
            batch.draw(texture, x, y, width, 1f)
            batch.draw(texture, x, y + height - 1f, width, 1f)
            batch.draw(texture, x, y, 1f, height)
            batch.draw(texture, x + width - 1f, y, 1f, height)
            batch.color = original
        }

        override fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
            return if (x >= 0f && x <= width && y >= 0f && y <= height) this else null
        }
    }
}
