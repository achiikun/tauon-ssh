package tauon.app.ui.components.misc;

import tauon.app.util.jnafilechooser.api.JnaFileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class NativeFileChooser{
    
    JnaFileChooser chooser = new JnaFileChooser();
    
    public NativeFileChooser(){
    
    }

    public File getSelectedFile() {
        return chooser.getSelectedFile();
    }

    public int showSaveDialog(Component appWindow) {
        return chooser.showSaveDialog(appWindow instanceof Window ? (Window) appWindow : SwingUtilities.windowForComponent(appWindow)) ? JFileChooser.APPROVE_OPTION : JFileChooser.CANCEL_OPTION;
    }

    public int showOpenDialog(Component appWindow) {
        return chooser.showOpenDialog(appWindow instanceof Window ? (Window) appWindow : SwingUtilities.windowForComponent(appWindow)) ? JFileChooser.APPROVE_OPTION : JFileChooser.CANCEL_OPTION;
    }

    public void setFileHidingEnabled(boolean b) {
        // Ignored
    }

    public void setFileSelectionMode(int directoriesOnly) {
        switch (directoriesOnly){
            case JFileChooser.FILES_ONLY:
                chooser.setMode(JnaFileChooser.Mode.Files);
                return;
            case JFileChooser.DIRECTORIES_ONLY:
                chooser.setMode(JnaFileChooser.Mode.Directories);
                return;
            case JFileChooser.FILES_AND_DIRECTORIES:
                chooser.setMode(JnaFileChooser.Mode.FilesAndDirectories);
                return;
        }
    }

    public void addChoosableFileFilter(FileNameExtensionFilter ppk) {
        chooser.addFilter(ppk);
    }

    public void setMultiSelectionEnabled(boolean b) {
        chooser.setMultiSelectionEnabled(b);
    }

    public File[] getSelectedFiles() {
        return chooser.getSelectedFiles();
    }
}
