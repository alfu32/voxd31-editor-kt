package com.voxd31.editor;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.xovd3i.editor.Voxd31Editor;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	private static boolean isResolutionArgument(String value) {
		if (value == null || !value.contains("x")) {
			return false;
		}
		String[] parts = value.toLowerCase().split("x", 2);
		if (parts.length != 2) {
			return false;
		}
		try {
			Integer.parseInt(parts[0]);
			Integer.parseInt(parts[1]);
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	public static void main (String[] arg) {
		Voxd31EditorVersion version = new Voxd31EditorVersion();

		System.out.println(String.format("version %s %s",version.getBuildDate(),version.getBuildVersion()));
		System.out.println("usage : voxcraft [filename] | [resolution filename]");
		System.out.println(String.format("arguments %d",arg.length));
		System.out.println(String.join(",",arg));
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setResizable(true);
		config.setDecorated(true);

		int width=1280,height=1024;
		if(arg.length > 0 && isResolutionArgument(arg[0])) {
			String[] resolution = arg[0].split("x");
			try{ width= Integer.parseInt(resolution[0]);} catch (NumberFormatException e) {}
			try{ height= Integer.parseInt(resolution[1]);} catch (NumberFormatException e) {}
		}
		config.setWindowedMode(width,height);

		String filename="default.vxdi";

		if(arg.length > 1 && isResolutionArgument(arg[0])) {
			filename=arg[1];
		} else if (arg.length > 0 && !isResolutionArgument(arg[0])) {
			filename = arg[0];
		}
		config.setTitle(String.format(
				"voxcraft   version : %s   file : [%s]",
				version.getBuildVersion(),
				filename
		));
		config.setWindowIcon("voxd31.icon.png");
		// width,height,
		new Lwjgl3Application(new Voxd31Editor(filename), config);
	}
}
