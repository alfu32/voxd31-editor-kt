package com.voxd31.editor

import com.voxd31.editor.exporters.resolveReadableHandle
import com.voxd31.editor.exporters.resolveWritableHandle

private val URI_SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*")

fun isDocumentUriPath(path: String): Boolean = URI_SCHEME_PATTERN.matches(path)

fun fallbackDisplayName(path: String): String {
    val normalized = path.replace('\\', '/')
    return normalized.substringAfterLast('/')
}

interface DocumentIoService {
    fun exists(path: String): Boolean

    fun readUtf8(path: String): String

    fun writeUtf8(path: String, text: String, append: Boolean = false)

    fun writeBytes(path: String, bytes: ByteArray)

    fun displayName(path: String): String? = null

    fun sidecarPath(path: String, suffix: String): String? {
        return if (isDocumentUriPath(path)) null else "$path$suffix"
    }
}

object DefaultDocumentIoService : DocumentIoService {
    override fun exists(path: String): Boolean = resolveReadableHandle(path).exists()

    override fun readUtf8(path: String): String = resolveReadableHandle(path).readString("UTF-8")

    override fun writeUtf8(path: String, text: String, append: Boolean) {
        resolveWritableHandle(path).writeString(text, append, "UTF-8")
    }

    override fun writeBytes(path: String, bytes: ByteArray) {
        resolveWritableHandle(path).writeBytes(bytes, false)
    }

    override fun displayName(path: String): String = fallbackDisplayName(path)
}
