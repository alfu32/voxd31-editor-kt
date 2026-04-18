package com.voxd31.editor.ui

import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.kotcrab.vis.ui.widget.VisImageTextButton

class AppImageTextButton(text: String, icon: Drawable?) : VisImageTextButton(text, icon) {
    override fun toString(): String = "AppImageTextButton"
}
