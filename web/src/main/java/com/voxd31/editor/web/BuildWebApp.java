package com.voxd31.editor.web;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.File;
import org.teavm.vm.TeaVMOptimizationLevel;

public final class BuildWebApp {
    private BuildWebApp() {
    }

    public static void main(String[] args) {
        String outputRoot = System.getProperty("voxcraft.web.output", "build/dist");
        String outputFolder = System.getProperty("voxcraft.web.folder", "webapp");
        String assetsPath = System.getProperty("voxcraft.web.assets", "../assets");
        String title = System.getProperty("voxcraft.web.title", "voxcraft");
        String outputName = System.getProperty("voxcraft.web.outputName", "voxcraft");
        boolean serve = Boolean.parseBoolean(System.getProperty("voxcraft.web.serve", "false"));
        int port = Integer.getInteger("voxcraft.web.port", 8766);

        WebBackend backend = new WebBackend()
            .setStartJettyAfterBuild(serve)
            .setJettyPort(port)
            .setHtmlTitle(title)
            .setWebappFolderName(outputFolder)
            .setHtmlWidth(0)
            .setHtmlHeight(0)
            .setCopyLoadingAsset(true);

        TeaCompiler compiler = new TeaCompiler(backend)
            .addAssets(new AssetFileHandle(assetsPath))
            .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
            .setMainClass(VoxcraftWebLauncher.class.getName())
            .setOutputName(outputName)
            .setObfuscated(false)
            .setDebugInformationGenerated(false)
            .setSourceMapsFileGenerated(false);

        compiler.build(new File(outputRoot));
    }
}
