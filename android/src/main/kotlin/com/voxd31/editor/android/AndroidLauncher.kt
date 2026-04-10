package com.voxd31.editor.android

import android.content.Intent
import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.xovd3i.editor.Voxd31Editor
import java.io.File
import java.io.FileOutputStream

class AndroidLauncher : AndroidApplication() {
    private lateinit var fileDialogService: AndroidFileDialogService

    private fun copyAssetIfMissing(assetName: String, target: File) {
        if (target.exists()) {
            return
        }
        target.parentFile?.mkdirs()
        assets.open(assetName).use { input ->
            FileOutputStream(target, false).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useCompass = false
            useAccelerometer = false
            useGyroscope = false
            useRotationVectorSensor = false
        }

        val appDir = (getExternalFilesDir(null) ?: filesDir).absoluteFile
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val modelFile = File(appDir, "default.vxdi")
        copyAssetIfMissing("default.vxdi", modelFile)
        fileDialogService = AndroidFileDialogService(this)

        initialize(
            Voxd31Editor(
                modelFile.absolutePath,
                fileDialogService,
                AndroidDocumentIoService(this),
                AndroidTouchInputDecorator()
            ),
            cfg
        )
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (this::fileDialogService.isInitialized && fileDialogService.onActivityResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
