package com.voxd31.editor.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

public final class VoxcraftWebLauncher {
    private VoxcraftWebLauncher() {
    }

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        config.showDownloadLogs = false;
        config.useGL30 = true;
        config.storagePrefix = "voxcraft";
        config.localStoragePrefix = "voxcraft";
        config.shouldEncodePreference = true;
        config.alpha = true;
        config.antialiasing = true;
        config.premultipliedAlpha = true;

        new WebApplication(new VoxcraftWebApp(), config);
    }
}
