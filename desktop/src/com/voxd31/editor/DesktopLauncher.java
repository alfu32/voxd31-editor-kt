package com.voxd31.editor;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.xovd3i.editor.Voxd31Editor;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("voxd31-editor");
		config.setResizable(true);
		config.setWindowedMode(1900,1200);
		System.out.println(String.format("arguments %d",arg.length));
		System.out.println(String.join(",",arg));
		new Lwjgl3Application(new Voxd31Editor(arg.length>0?arg[0]:"default.vxdi"), config);
	}
}