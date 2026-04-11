package com.voxd31.editor.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.voxd31.editor.DefaultDocumentIoService
import com.voxd31.editor.DocumentIoService
import java.io.OutputStream
import java.io.FileNotFoundException

class AndroidDocumentIoService(
    private val context: Context
) : DocumentIoService {
    override fun exists(path: String): Boolean {
        if (!isContentDocumentPath(path)) {
            return DefaultDocumentIoService.exists(path)
        }
        return try {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(path), "r")?.use { true } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun readUtf8(path: String): String {
        if (!isContentDocumentPath(path)) {
            return DefaultDocumentIoService.readUtf8(path)
        }
        return openInputStream(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    override fun writeUtf8(path: String, text: String, append: Boolean) {
        if (!isContentDocumentPath(path)) {
            DefaultDocumentIoService.writeUtf8(path, text, append)
            return
        }
        if (append) {
            val existing = if (exists(path)) readUtf8(path) else ""
            writeBytes(path, (existing + text).toByteArray(Charsets.UTF_8))
            return
        }
        writeBytes(path, text.toByteArray(Charsets.UTF_8))
    }

    override fun writeBytes(path: String, bytes: ByteArray) {
        if (!isContentDocumentPath(path)) {
            DefaultDocumentIoService.writeBytes(path, bytes)
            return
        }
        openOutputStream(path).use { output ->
            output.write(bytes)
            output.flush()
        }
    }

    override fun displayName(path: String): String? {
        if (!isContentDocumentPath(path)) {
            return DefaultDocumentIoService.displayName(path)
        }
        val uri = Uri.parse(path)
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            } ?: uri.lastPathSegment
        } catch (_: Throwable) {
            uri.lastPathSegment
        }
    }

    override fun sidecarPath(path: String, suffix: String): String? {
        if (isContentDocumentPath(path)) {
            return null
        }
        return DefaultDocumentIoService.sidecarPath(path, suffix)
    }

    private fun openInputStream(path: String) = context.contentResolver.openInputStream(Uri.parse(path))
        ?: throw FileNotFoundException("Unable to open input stream for $path")

    private fun openOutputStream(path: String): OutputStream {
        val uri = Uri.parse(path)
        return openOutputStream(uri, "rwt")
            ?: openOutputStream(uri, "wt")
            ?: openOutputStream(uri, "w")
            ?: openOutputStream(uri, null)
            ?: throw FileNotFoundException("Unable to open output stream for $path")
    }

    private fun openOutputStream(uri: Uri, mode: String?): OutputStream? {
        return try {
            if (mode == null) {
                context.contentResolver.openOutputStream(uri)
            } else {
                context.contentResolver.openOutputStream(uri, mode)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isContentDocumentPath(path: String): Boolean {
        val scheme = Uri.parse(path).scheme ?: return false
        return scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)
    }
}
