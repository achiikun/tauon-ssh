package tauon.app.ui.containers.session.pages.files.view;

import tauon.app.services.BookmarkManager;
import tauon.app.services.SettingsService;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.local.LocalFileBrowserView;
import tauon.app.ui.containers.session.pages.files.view.folderview.FolderView;
import tauon.app.util.misc.PathUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static tauon.app.services.LanguageService.getBundle;

public class OverflowMenuHandler {
    private final JCheckBoxMenuItem mShowHiddenFiles;
    private final AtomicBoolean sortingChanging = new AtomicBoolean(false);
    private final KeyStroke ksHideShow;
    private final AbstractAction aHideShow;
    private final JPopupMenu popup;
    private final AbstractFileBrowserView fileBrowserView;
    private final JMenu favouriteLocations;
    private final FileBrowser fileBrowser;
    private FolderView folderView;

    public OverflowMenuHandler(AbstractFileBrowserView fileBrowserView, FileBrowser fileBrowser) {
        this.fileBrowserView = fileBrowserView;
        this.fileBrowser = fileBrowser;
        ksHideShow = KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);

        mShowHiddenFiles = new JCheckBoxMenuItem(getBundle().getString("app.files.action.show_hidden_files"));
        mShowHiddenFiles.setSelected(SettingsService.getSettings().isShowHiddenFilesByDefault());

        aHideShow = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mShowHiddenFiles.setSelected(!mShowHiddenFiles.isSelected());
                hideOptAction();
            }
        };

        mShowHiddenFiles.addActionListener(e -> {
            hideOptAction();
        });
        mShowHiddenFiles.setAccelerator(ksHideShow);

        this.favouriteLocations = new JMenu(getBundle().getString("bookmarks"));

        popup = new JPopupMenu();

        popup.add(mShowHiddenFiles);
        popup.add(favouriteLocations);

        loadFavourites();
    }

    public void loadFavourites() {
        this.favouriteLocations.removeAll();
        String id = fileBrowserView instanceof LocalFileBrowserView ? null : fileBrowser.getInfo().getId();
        for (String path : BookmarkManager.getInstance().getBookmarks(id)) {
            JMenuItem item = new JMenuItem(PathUtils.getFileName(path));
            item.setName(path);
            this.favouriteLocations.add(item);
            item.addActionListener(e -> {
                fileBrowserView.render(item.getName());
            });
        }
    }

    private void hideOptAction() {
        folderView.setShowHiddenFiles(mShowHiddenFiles.isSelected());
    }

    public JPopupMenu getOverflowMenu() {
        return popup;
    }

    public void setFolderView(FolderView folderView) {
        InputMap map = folderView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap act = folderView.getActionMap();
        this.folderView = folderView;
        map.put(ksHideShow, "ksHideShow");
        act.put("ksHideShow", aHideShow);
    }
}
