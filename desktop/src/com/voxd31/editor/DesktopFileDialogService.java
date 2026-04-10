package com.voxd31.editor;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

public final class DesktopFileDialogService implements FileDialogService {
    @Override
    public boolean isSupported() {
        return !GraphicsEnvironment.isHeadless();
    }

    @Override
    public String openFile(String title, String directoryHint, String defaultFileName, Set<String> allowedExtensions) {
        return showDialog(title, FileDialog.LOAD, directoryHint, defaultFileName, allowedExtensions);
    }

    @Override
    public String saveFile(String title, String directoryHint, String defaultFileName, Set<String> allowedExtensions) {
        return showDialog(title, FileDialog.SAVE, directoryHint, defaultFileName, allowedExtensions);
    }

    private String showDialog(
            String title,
            int mode,
            String directoryHint,
            String defaultFileName,
            Set<String> allowedExtensions
    ) {
        if (!isSupported()) {
            return null;
        }

        final File[] result = new File[1];
        Runnable openDialog = () -> {
            FileDialog dialog = new FileDialog((Frame) null, title, mode);
            try {
                if (directoryHint != null && !directoryHint.isBlank()) {
                    dialog.setDirectory(directoryHint);
                }
                if (defaultFileName != null && !defaultFileName.isBlank()) {
                    dialog.setFile(defaultFileName);
                }
                if (mode == FileDialog.LOAD && allowedExtensions != null && !allowedExtensions.isEmpty()) {
                    dialog.setFilenameFilter(buildFilenameFilter(allowedExtensions));
                }
                dialog.setVisible(true);
                if (dialog.getDirectory() != null && dialog.getFile() != null) {
                    result[0] = new File(dialog.getDirectory(), dialog.getFile()).getAbsoluteFile();
                }
            } finally {
                dialog.dispose();
            }
        };

        try {
            if (EventQueue.isDispatchThread()) {
                openDialog.run();
            } else {
                EventQueue.invokeAndWait(openDialog);
            }
            return result[0] != null ? result[0].getAbsolutePath() : null;
        } catch (HeadlessException ignored) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private FilenameFilter buildFilenameFilter(Set<String> allowedExtensions) {
        return (dir, name) -> {
            int dot = name.lastIndexOf('.');
            if (dot < 0 || dot == name.length() - 1) {
                return false;
            }
            String ext = name.substring(dot + 1).toLowerCase();
            return allowedExtensions.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(ext));
        };
    }
}
