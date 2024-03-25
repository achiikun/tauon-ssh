package tauon.app.ui.containers.session.pages.files.local;

import tauon.app.App;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.services.BookmarkManager;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.view.FolderView;
import tauon.app.util.misc.PathUtils;
import tauon.app.util.misc.PlatformUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static tauon.app.services.LanguageService.getBundle;

public class LocalMenuHandler {
    private final FileBrowser fileBrowser;
    private final LocalFileOperations fileOperations;
    private final LocalFileBrowserView fileBrowserView;
    private JMenuItem mOpenInNewTab, mRename, mDelete, mNewFile, mNewFolder, mCopy, mPaste, mCut, mAddToFav, mOpen,
            mOpenInFileExplorer;
    private FolderView folderView;

    public LocalMenuHandler(FileBrowser fileBrowser, LocalFileBrowserView fileBrowserView) {
        this.fileBrowser = fileBrowser;
        this.fileOperations = new LocalFileOperations();
        this.fileBrowserView = fileBrowserView;
    }

    public void initMenuHandler(FolderView folderView) {
        this.folderView = folderView;
        InputMap map = folderView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap act = folderView.getActionMap();
        this.initMenuItems();
    }

    private void initMenuItems() {
        mOpen = new JMenuItem(getBundle().getString("open"));
        mOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open();
            }
        });
        mOpenInNewTab = new JMenuItem(getBundle().getString("open_new_tab"));
        mOpenInNewTab.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openNewTab();
            }
        });

        mOpenInFileExplorer = new JMenuItem(
                App.IS_WINDOWS ? "Open in Windows Explorer" : (App.IS_MAC ? "Open in Finder" : "Open in File Browser"));
        mOpenInFileExplorer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FileInfo file = folderView.getSelectedFiles()[0];
                    if(file.isDirectory()){
                        PlatformUtils.openFolderInExplorer(folderView.getSelectedFiles()[0].getPath(), null);
                    }else{
                        File file1 = new File(file.getPath());
                        PlatformUtils.openFolderInExplorer(file1.getParent(), file1.getName());
                    }
                    
                } catch (FileNotFoundException e1) {
                    // TODO handle exception
                    e1.printStackTrace();
                }
            }
        });

        mRename = new JMenuItem(getBundle().getString("rename"));
        mRename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rename(folderView.getSelectedFiles()[0], fileBrowserView.getCurrentDirectory());
            }
        });

        mDelete = new JMenuItem(getBundle().getString("delete"));
        mDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                delete(folderView.getSelectedFiles());
            }
        });

        mNewFile = new JMenuItem(getBundle().getString("new_file"));
        mNewFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile();
            }
        });

        mNewFolder = new JMenuItem(getBundle().getString("new_folder"));
        mNewFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFolder(fileBrowserView.getCurrentDirectory());
            }
        });

        mCopy = new JMenuItem(getBundle().getString("copy"));
        mCopy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        mPaste = new JMenuItem(getBundle().getString("paste"));
        mPaste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        mCut = new JMenuItem(getBundle().getString("cut"));
        mCut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        mAddToFav = new JMenuItem(getBundle().getString("bookmark"));
        mAddToFav.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addToFavourites();
            }
        });
    }

    public void createMenu(JPopupMenu popup, FileInfo[] selectedFiles) {
        createMenuContext(popup, selectedFiles);
    }

    private void createMenuContext(JPopupMenu popup, FileInfo[] files) {
        popup.removeAll();
        int selectionCount = files.length;
        createBuitinItems1(selectionCount, popup, files);
        createBuitinItems2(selectionCount, popup);
    }

    private void createBuitinItems1(int selectionCount, JPopupMenu popup, FileInfo[] selectedFiles) {
        if (selectionCount == 1) {
            if (selectedFiles[0].getType() == FileType.File || selectedFiles[0].getType() == FileType.FileLink) {
                popup.add(mOpen);
                popup.add(mOpenInFileExplorer);
            }
            if (selectedFiles[0].getType() == FileType.Directory || selectedFiles[0].getType() == FileType.DirLink) {
                popup.add(mOpenInNewTab);
                popup.add(mOpenInFileExplorer);
            }
            popup.add(mRename);
        }

    }

    private void createBuitinItems2(int selectionCount, JPopupMenu popup) {
        popup.add(mNewFolder);
        popup.add(mNewFile);
        // check only if folder is selected
        popup.add(mAddToFav);
    }

    private void open() {
        FileInfo[] files = folderView.getSelectedFiles();
        if (files.length == 1) {
            FileInfo file = files[0];
            if (file.getType() == FileType.FileLink || file.getType() == FileType.File) {
            }
        }
    }

    private void openNewTab() {
        FileInfo[] files = folderView.getSelectedFiles();
        if (files.length == 1) {
            FileInfo file = files[0];
            if (file.getType() == FileType.Directory || file.getType() == FileType.DirLink) {
                fileBrowser.openLocalFileBrowserView(file.getPath(), this.fileBrowserView.getOrientation());
            }
        }
    }

    private void rename(FileInfo info, String baseFolder) {
        String text = JOptionPane.showInputDialog(getBundle().getString("enter_new_name"), info.getName());
        if (text != null && text.length() > 0) {
            renameAsync(info.getPath(), PathUtils.combineUnix(PathUtils.getParent(info.getPath()), text), baseFolder);
        }
    }

    private void renameAsync(String oldName, String newName, String baseFolder) {
        fileBrowser.getHolder().executor.submit(() -> {
            fileBrowser.disableUi();
            if (fileOperations.rename(oldName, newName)) {
                fileBrowserView.render(baseFolder);
            } else {
                fileBrowser.enableUi();
            }
        });
    }

    private void delete(FileInfo[] selectedFiles) {
        fileBrowser.getHolder().executor.submit(() -> {
            fileBrowser.disableUi();
            for (FileInfo f : selectedFiles) {
                try {
                    new LocalFileSystem().delete(f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            fileBrowser.enableUi();
        });
    }

    private void newFile() {
        fileBrowser.getHolder().executor.submit(() -> {
            fileBrowser.disableUi();
            String baseFolder = fileBrowserView.getCurrentDirectory();
            if (fileOperations.newFile(baseFolder)) {
                fileBrowserView.render(baseFolder);
            } else {
                fileBrowser.enableUi();
            }
        });
    }

    private void newFolder(String currentDirectory) {
        fileBrowser.getHolder().executor.submit(() -> {
            fileBrowser.disableUi();
            String baseFolder = currentDirectory;
            if (fileOperations.newFolder(baseFolder)) {
                fileBrowserView.render(baseFolder);
            } else {
                fileBrowser.enableUi();
            }
        });
    }

    private void addToFavourites() {
        FileInfo[] arr = folderView.getSelectedFiles();

        if (arr.length > 0) {
            BookmarkManager.addEntry(null,
                    Arrays.asList(arr).stream()
                            .filter(a -> a.getType() == FileType.DirLink || a.getType() == FileType.Directory)
                            .map(a -> a.getPath()).collect(Collectors.toList()));
        } else if (arr.length == 0) {
            BookmarkManager.addEntry(null, fileBrowserView.getCurrentDirectory());
        }

        this.fileBrowserView.getOverflowMenuHandler().loadFavourites();

    }

    public JPopupMenu createAddressPopup() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem mOpenInNewTab = new JMenuItem(getBundle().getString("open_new_tab"));
        JMenuItem mCopyPath = new JMenuItem(getBundle().getString("copy_path"));
        JMenuItem mOpenInTerminal = new JMenuItem(getBundle().getString("open_in_terminal"));
        JMenuItem mBookmark = new JMenuItem(getBundle().getString("bookmark"));
        popupMenu.add(mOpenInNewTab);
        popupMenu.add(mCopyPath);
        popupMenu.add(mOpenInTerminal);
        popupMenu.add(mBookmark);

        mOpenInNewTab.addActionListener(e -> {
            String path = popupMenu.getName();
        });

        mOpenInTerminal.addActionListener(e -> {

        });

        mCopyPath.addActionListener(e -> {
            String path = popupMenu.getName();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
        });

        mBookmark.addActionListener(e -> {
            String path = popupMenu.getName();
            BookmarkManager.addEntry(null, path);
        });
        return popupMenu;
    }
}
