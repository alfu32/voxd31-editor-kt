package com.voxd31.editor

interface FileDialogService {
    fun isSupported(): Boolean

    fun chooseOption(
        title: String,
        message: String,
        options: List<String>,
        defaultOption: String? = null
    ): String?

    fun openFile(
        title: String,
        directoryHint: String? = null,
        defaultFileName: String? = null,
        allowedExtensions: Set<String> = emptySet()
    ): String?

    fun saveFile(
        title: String,
        directoryHint: String? = null,
        defaultFileName: String? = null,
        allowedExtensions: Set<String> = emptySet()
    ): String?
}

object NoopFileDialogService : FileDialogService {
    override fun isSupported(): Boolean = false

    override fun chooseOption(
        title: String,
        message: String,
        options: List<String>,
        defaultOption: String?
    ): String? = null

    override fun openFile(
        title: String,
        directoryHint: String?,
        defaultFileName: String?,
        allowedExtensions: Set<String>
    ): String? = null

    override fun saveFile(
        title: String,
        directoryHint: String?,
        defaultFileName: String?,
        allowedExtensions: Set<String>
    ): String? = null
}
