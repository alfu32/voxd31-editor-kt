package com.voxd31.editor.exporters

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

private val WINDOWS_ABSOLUTE_PATH = Regex("^[A-Za-z]:[\\\\/].*")

private fun isAbsolutePath(filename: String): Boolean {
    return filename.startsWith("/") || WINDOWS_ABSOLUTE_PATH.matches(filename)
}

fun resolveReadableHandle(filename: String): FileHandle {
    if (isAbsolutePath(filename)) {
        return Gdx.files.absolute(filename)
    }

    val localHandle = Gdx.files.local(filename)
    if (localHandle.exists()) {
        return localHandle
    }

    val internalHandle = Gdx.files.internal(filename)
    if (internalHandle.exists()) {
        return internalHandle
    }

    return localHandle
}

fun resolveWritableHandle(filename: String): FileHandle {
    return if (isAbsolutePath(filename)) {
        Gdx.files.absolute(filename)
    } else {
        Gdx.files.local(filename)
    }
}

fun appendTextFile(filename: String, text: String) {
    if (text.isEmpty()) {
        return
    }
    resolveWritableHandle(filename).writeString(text, true, "UTF-8")
}
