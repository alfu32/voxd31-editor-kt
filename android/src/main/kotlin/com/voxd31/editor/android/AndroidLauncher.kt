package com.voxd31.editor.android

import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.xovd3i.editor.Voxd31Editor
import java.io.File
import java.io.FileOutputStream

class AndroidLauncher : AndroidApplication() {
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

        initialize(Voxd31Editor(modelFile.absolutePath), cfg)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true)
    }
}
