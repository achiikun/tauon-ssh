/* This file is part of JnaFileChooser.
 *
 * JnaFileChooser is free software: you can redistribute it and/or modify it
 * under the terms of the new BSD license.
 *
 * JnaFileChooser is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package tauon.app.util.jnafilechooser.api;

import com.sun.jna.Platform;
import org.apache.commons.io.filefilter.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * JnaFileChooser is a wrapper around the native Windows file chooser
 * and folder browser that falls back to the Swing JFileChooser on platforms
 * other than Windows or if the user chooses a combination of features
 * that are not supported by the native dialogs (for example multiple
 * selection of directories).
 *
 * Example:
 * JnaFileChooser fc = new JnaFileChooser();
 * fc.setFilter("All Files", "*");
 * fc.setFilter("Pictures", "jpg", "jpeg", "gif", "png", "bmp");
 * fc.setMultiSelectionEnabled(true);
 * fc.setMode(JnaFileChooser.Mode.FilesAndDirectories);
 * if (fc.showOpenDialog(parent)) {
 *     Files[] selected = fc.getSelectedFiles();
 *     // do something with selected
 * }
 *
 * @see JFileChooser, WindowsFileChooser, WindowsFileBrowser
 */
public class JnaFileChooser
{
	private static enum Action { Open, Save }

	/**
	 * the availabe selection modes of the dialog
	 */
	public static enum Mode {
		Files(JFileChooser.FILES_ONLY),
		Directories(JFileChooser.DIRECTORIES_ONLY),
		FilesAndDirectories(JFileChooser.FILES_AND_DIRECTORIES);
		private int jFileChooserValue;
		private Mode(int jfcv) {
			this.jFileChooserValue = jfcv;
		}
		public int getJFileChooserValue() {
			return jFileChooserValue;
		}
	}

	protected File[] selectedFiles;
	protected File currentDirectory;
	protected ArrayList<FileNameExtensionFilter> filters;
	protected boolean multiSelectionEnabled;
	protected Mode mode;

	/**
	 * creates a new file chooser with multiselection disabled and mode set
	 * to allow file selection only.
	 */
	public JnaFileChooser() {
		filters = new ArrayList<FileNameExtensionFilter>();
		multiSelectionEnabled = false;
		mode = Mode.Files;
		selectedFiles = new File[] { null };
	}

	/**
	 * creates a new file chooser with the specified initial directory
	 *
	 * @param currentDirectory the initial directory
	 */
	public JnaFileChooser(File currentDirectory) {
		this();
		this.currentDirectory = currentDirectory.isDirectory() ?
			currentDirectory : currentDirectory.getParentFile();
	}

	/**
	 * creates a new file chooser with the specified initial directory
	 *
	 * @param currentDirectoryPath the initial directory
	 */
	public JnaFileChooser(String currentDirectoryPath) {
		this(currentDirectoryPath != null ?
			new File(currentDirectoryPath) : null);
	}

	public void setCurrentDirectory(File currentDirectory) {
		this.currentDirectory = currentDirectory.isDirectory() ?
				currentDirectory : currentDirectory.getParentFile();
	}

	/**
	 * shows a dialog for opening files
	 *
	 * @param parent the parent window
	 *
	 * @return true if the user clicked OK
	 */
	public boolean showOpenDialog(Window parent) {
		return showDialog(parent, Action.Open);
	}

	/**
	 * shows a dialog for saving files
	 *
	 * @param parent the parent window
	 *
	 * @return true if the user clicked OK
	 */
	public boolean showSaveDialog(Window parent) {
		return showDialog(parent, Action.Save);
	}

	private boolean showDialog(Window parent, Action action) {
		// native windows filechooser doesn't support mixed selection mode
		if (Platform.isWindows() && mode != Mode.FilesAndDirectories) {
			// windows filechooser can only multiselect files
			if (multiSelectionEnabled && mode == Mode.Files) {
				// TODO Here we would use the native windows dialog
				// to choose multiple files. However I haven't been able
				// to get it to work properly yet because it requires
				// tricky callback magic and somehow this didn't work for me
				// quite as documented (probably because I messed something up).
				// Because I don't need this feature right now I've put it on
				// hold to get on with stuff.
				// Example code: http://support.microsoft.com/kb/131462/en-us
				// GetOpenFileName: http://msdn.microsoft.com/en-us/library/ms646927.aspx
				// OFNHookProc: http://msdn.microsoft.com/en-us/library/ms646931.aspx
				// CDN_SELCHANGE: http://msdn.microsoft.com/en-us/library/ms646865.aspx
				// SendMessage: http://msdn.microsoft.com/en-us/library/ms644950.aspx
			}
			else if (!multiSelectionEnabled) {
				if (mode == Mode.Files) {
					return showWindowsFileChooser(parent, action);
				}
				else if (mode == Mode.Directories) {
					return showWindowsFolderBrowser(parent);
				}
			}
		}else if(Platform.isLinux()){
			return showAWTFileChooser(parent, action);
		}

		// fallback to Swing
		return showSwingFileChooser(parent, action);
	}
	
	private boolean showAWTFileChooser(Window parent, Action action) {
		final java.awt.FileDialog fc = new FileDialog((Frame) parent.getOwner());
		if(currentDirectory != null)
			fc.setDirectory(currentDirectory.getPath());
		fc.setMultipleMode(multiSelectionEnabled);
		fc.setMode(action == Action.Open ? FileDialog.LOAD : FileDialog.SAVE);
		
		if(mode == Mode.Directories){
			ArrayList<IOFileFilter> ord = new ArrayList<>();
			for (final FileNameExtensionFilter spec : filters) {
				ord.add(new SuffixFileFilter(spec.getExtensions()));
			}
			if(ord.isEmpty()){
				fc.setFilenameFilter(DirectoryFileFilter.DIRECTORY);
			}else {
				OrFileFilter ordFileFilter = new OrFileFilter(ord);
				fc.setFilenameFilter(new AndFileFilter(DirectoryFileFilter.DIRECTORY, ordFileFilter));
			}
		}else if(mode == Mode.Files){
			ArrayList<IOFileFilter> ord = new ArrayList<>();
			for (final FileNameExtensionFilter spec : filters) {
				ord.add(new SuffixFileFilter(spec.getExtensions()));
			}
			if(ord.isEmpty()){
				fc.setFilenameFilter(FileFileFilter.FILE);
			}else {
				OrFileFilter ordFileFilter = new OrFileFilter(ord);
				fc.setFilenameFilter(new AndFileFilter(FileFileFilter.FILE, ordFileFilter));
			}
		}else{
			ArrayList<IOFileFilter> ord = new ArrayList<>();
			for (final FileNameExtensionFilter spec : filters) {
				ord.add(new SuffixFileFilter(spec.getExtensions()));
			}
			if(ord.isEmpty()) {
				OrFileFilter ordFileFilter = new OrFileFilter(ord);
				fc.setFilenameFilter(ordFileFilter);
			}
		}
		
		fc.setVisible(true);
		
		if(multiSelectionEnabled){
			File[] result = fc.getFiles();
			if(result.length == 0)
				return false;
			selectedFiles = result;
			currentDirectory = new File(fc.getDirectory());
		}else{
			String result = fc.getFile();
			if(result == null)
				return false;
			selectedFiles = new File[] { new File(fc.getDirectory(), result) };
		}
		
		currentDirectory = new File(fc.getDirectory());
		return true;
		
	}

	private boolean showSwingFileChooser(Window parent, Action action) {
		final JFileChooser fc = new JFileChooser(currentDirectory);
		fc.setMultiSelectionEnabled(multiSelectionEnabled);
		fc.setFileSelectionMode(mode.getJFileChooserValue());

		// build filters
		boolean useAcceptAllFilter = filters.isEmpty();
		for (final FileNameExtensionFilter spec : filters) {
			fc.addChoosableFileFilter(spec);
		}
		fc.setAcceptAllFileFilterUsed(useAcceptAllFilter);
		
		int result = -1;
		if (action == Action.Open) {
			result = fc.showOpenDialog(parent);
		}
		else {
			result = fc.showSaveDialog(parent);
		}
		if (result == JFileChooser.APPROVE_OPTION) {
			selectedFiles = multiSelectionEnabled ?
				fc.getSelectedFiles() : new File[] { fc.getSelectedFile() };
			currentDirectory = fc.getCurrentDirectory();
			return true;
		}

		return false;
	}

	private boolean showWindowsFileChooser(Window parent, Action action) {
		final WindowsFileChooser fc = new WindowsFileChooser(currentDirectory);
		ArrayList<String[]> list = new ArrayList<>();
		for(FileNameExtensionFilter f: filters){
			ArrayList<String> ss = new ArrayList<>();
			ss.add(f.getDescription());
            Collections.addAll(ss, f.getExtensions());
			list.add(ss.toArray(new String[0]));
		}
		fc.setFilters(list);
		final boolean result = fc.showDialog(parent, action == Action.Open);
		if (result) {
			selectedFiles = new File[] { fc.getSelectedFile() };
			currentDirectory = fc.getCurrentDirectory();
		}
		return result;
	}

	private boolean showWindowsFolderBrowser(Window parent) {
		final WindowsFolderBrowser fb = new WindowsFolderBrowser();
		final File file = fb.showDialog(parent);
		if (file != null) {
			selectedFiles = new File[] { file };
			currentDirectory = file.getParentFile() != null ?
				file.getParentFile() : file;
			return true;
		}

		return false;
	}

	/**
	 * add a filter to the user-selectable list of file filters
	 *
	 * @param values you must pass at least 2 arguments, the first argument
	 *               is the name of this filter and the remaining arguments
	 *               are the file extensions.
	 *
	 * @throws IllegalArgumentException if less than 2 arguments are passed
	 */
	public void addFilter(String ... values) {
		if (values.length < 2) {
			throw new IllegalArgumentException();
		}
		filters.add(new FileNameExtensionFilter(values[0], Arrays.copyOfRange(values, 1, values.length)));
	}
	
	public void addFilter(FileNameExtensionFilter value) {
		filters.add(value);
	}

	/**
	 * sets the selection mode
	 *
	 * @param mode the selection mode
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Mode getMode() {
		return mode;
	}

	/**
	 * sets whether to enable multiselection
	 *
	 * @param enabled true to enable multiselection, false to disable it
	 */
	public void setMultiSelectionEnabled(boolean enabled) {
		this.multiSelectionEnabled = enabled;
	}

	public boolean isMultiSelectionEnabled() {
		return multiSelectionEnabled;
	}

	public File[] getSelectedFiles() {
		return selectedFiles;
	}

	public File getSelectedFile() {
		return selectedFiles[0];
	}

	public File getCurrentDirectory() {
		return currentDirectory;
	}
}
