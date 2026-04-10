package com.voxd31.editor.android

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.voxd31.editor.FileDialogService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class AndroidFileDialogService(
    private val activity: AndroidLauncher
) : FileDialogService {
    companion object {
        private const val REQUEST_OPEN_DOCUMENT = 4101
        private const val REQUEST_CREATE_DOCUMENT = 4102
    }

    private data class PendingDocumentRequest(
        val requestCode: Int,
        val latch: CountDownLatch = CountDownLatch(1),
        val value: AtomicReference<String?> = AtomicReference(null)
    ) {
        fun complete(path: String?) {
            value.compareAndSet(null, path)
            latch.countDown()
        }

        fun await(): String? {
            latch.await()
            return value.get()
        }
    }

    @Volatile
    private var pendingDocumentRequest: PendingDocumentRequest? = null

    override fun isSupported(): Boolean = true

    override fun chooseOption(
        title: String,
        message: String,
        options: List<String>,
        defaultOption: String?
    ): String? {
        if (options.isEmpty()) {
            return null
        }
        val defaultIndex = options.indexOf(defaultOption).takeIf { it >= 0 } ?: 0
        val selection = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyedCompat()) {
                latch.countDown()
                return@runOnUiThread
            }
            val currentIndex = intArrayOf(defaultIndex)
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setSingleChoiceItems(options.toTypedArray(), defaultIndex) { _, which ->
                    currentIndex[0] = which
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    selection.set(options[currentIndex[0]])
                    dialog.dismiss()
                    latch.countDown()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    latch.countDown()
                }
                .setOnCancelListener {
                    latch.countDown()
                }
                .show()
        }
        latch.await()
        return selection.get()
    }

    override fun openFile(
        title: String,
        directoryHint: String?,
        defaultFileName: String?,
        allowedExtensions: Set<String>
    ): String? {
        val mimeTypes = allowedExtensions.toMimeTypes()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = when {
                mimeTypes.isEmpty() -> "*/*"
                mimeTypes.size == 1 -> mimeTypes.first()
                else -> "*/*"
            }
            if (mimeTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
            }
            parseDocumentUri(directoryHint)?.let { uri ->
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
        return launchDocumentIntent(REQUEST_OPEN_DOCUMENT, intent, title)
    }

    override fun saveFile(
        title: String,
        directoryHint: String?,
        defaultFileName: String?,
        allowedExtensions: Set<String>
    ): String? {
        val mimeTypes = allowedExtensions.toMimeTypes()
        val suggestedFileName = ensureAllowedExtension(defaultFileName ?: "voxcraft", allowedExtensions)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeTypes.firstOrNull() ?: "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, suggestedFileName)
            parseDocumentUri(directoryHint)?.let { uri ->
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
        return launchDocumentIntent(REQUEST_CREATE_DOCUMENT, intent, title)
    }

    @Suppress("DEPRECATION")
    private fun launchDocumentIntent(requestCode: Int, intent: Intent, title: String): String? {
        val pending = PendingDocumentRequest(requestCode)
        synchronized(this) {
            if (pendingDocumentRequest != null) {
                return null
            }
            pendingDocumentRequest = pending
        }
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyedCompat()) {
                completePendingDocumentRequest(requestCode, null)
                return@runOnUiThread
            }
            try {
                activity.startActivityForResult(Intent.createChooser(intent, title), requestCode)
            } catch (_: Throwable) {
                completePendingDocumentRequest(requestCode, null)
            }
        }
        return pending.await()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val pending = synchronized(this) {
            pendingDocumentRequest?.takeIf { it.requestCode == requestCode }
        } ?: return false
        val path = if (resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                takePersistablePermission(uri, data.flags)
            }?.toString()
        } else {
            null
        }
        completePendingDocumentRequest(pending.requestCode, path)
        return true
    }

    private fun completePendingDocumentRequest(requestCode: Int, path: String?) {
        val pending = synchronized(this) {
            pendingDocumentRequest?.takeIf { it.requestCode == requestCode }?.also {
                pendingDocumentRequest = null
            }
        } ?: return
        pending.complete(path)
    }

    private fun takePersistablePermission(uri: Uri, flags: Int) {
        val persistableFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (persistableFlags == 0) {
            return
        }
        try {
            activity.contentResolver.takePersistableUriPermission(uri, persistableFlags)
        } catch (_: SecurityException) {
        }
    }

    private fun parseDocumentUri(path: String?): Uri? {
        if (path.isNullOrBlank()) {
            return null
        }
        return try {
            Uri.parse(path)
        } catch (_: Throwable) {
            null
        }
    }

    private fun ensureAllowedExtension(name: String, allowedExtensions: Set<String>): String {
        if (allowedExtensions.isEmpty()) {
            return name
        }
        val normalized = name.trim()
        val existingExtension = normalized.substringAfterLast('.', "")
        if (existingExtension.isNotEmpty() && allowedExtensions.any { it.equals(existingExtension, ignoreCase = true) }) {
            return normalized
        }
        return "$normalized.${allowedExtensions.first()}"
    }

    private fun Set<String>.toMimeTypes(): List<String> {
        return mapNotNull { extension ->
            when (extension.lowercase()) {
                "vxdi" -> "text/plain"
                "svg" -> "image/svg+xml"
                "png" -> "image/png"
                "obj", "stl", "stlb", "stla", "dae", "dxf", "3mf", "amf", "fbx", "glb" -> "application/octet-stream"
                "gltf" -> "model/gltf+json"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }.distinct()
    }
}

private fun AndroidLauncher.isDestroyedCompat(): Boolean {
    return try {
        isDestroyed
    } catch (_: Throwable) {
        false
    }
}
