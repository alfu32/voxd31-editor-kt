package com.voxd31.editor;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.xovd3i.editor.Voxd31Editor;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Voxd31EditorVersion version = new Voxd31EditorVersion();

		System.out.println(String.format("version %s %s",version.getBuildDate(),version.getBuildVersion()));
		System.out.println(String.format("usage : v0x3d1 <resolution>[1280x1024] <filename>",arg.length));
		System.out.println(String.format("arguments %d",arg.length));
		System.out.println(String.join(",",arg));
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setResizable(true);
		config.setDecorated(true);

		int width=1280,height=1024;
		if(arg.length > 0) {
			String[] resolution = arg[0].split("x");
			try{ width= Integer.parseInt(resolution[0]);} catch (NumberFormatException e) {}
			try{ height= Integer.parseInt(resolution[1]);} catch (NumberFormatException e) {}
		}
		config.setWindowedMode(width,height);

		String filename="default.vxdi";

		if(arg.length > 1) {
			filename=arg[1];
		}
		config.setTitle(String.format(
				" V0XD31   verision : %s   file : [%s]",
				version.getBuildVersion(),
				filename
		));
		config.setWindowIcon("voxd31.icon.png");
		// width,height,
		new Lwjgl3Application(new Voxd31Editor(filename), config);
	}
}