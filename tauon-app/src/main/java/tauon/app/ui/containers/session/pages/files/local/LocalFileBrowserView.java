package tauon.app.ui.containers.session.pages.files.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsService;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.view.addressbar.AddressBar;
import tauon.app.ui.containers.session.pages.files.transfer.DndTransferData;
import tauon.app.ui.containers.session.pages.files.transfer.DndTransferHandler;
import tauon.app.ui.containers.session.pages.tools.keys.KeyPage;
import tauon.app.util.misc.PathUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

public class LocalFileBrowserView extends AbstractFileBrowserView {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileBrowserView.class);
    
    private final LocalMenuHandler menuHandler;
    private final DndTransferHandler transferHandler;
    private final JPopupMenu addressPopup;
    private LocalFileSystem fs;

    public LocalFileBrowserView(FileBrowser fileBrowser, String initialPath, PanelOrientation orientation) {
        super(orientation, fileBrowser);
        this.menuHandler = new LocalMenuHandler(fileBrowser, this);
        this.menuHandler.initMenuHandler(this.folderView);
        this.transferHandler = new DndTransferHandler(this);
        this.folderView.setTransferHandler(transferHandler);
        this.folderView.setFolderViewTransferHandler(transferHandler);
        this.addressPopup = menuHandler.createAddressPopup();

        if (this.fileBrowser.getInfo().getLocalFolder() != null && this.fileBrowser.getInfo().getLocalFolder().trim().length() > 1) {
            this.path = fileBrowser.getInfo().getLocalFolder();
        } else if (initialPath != null) {
            this.path = initialPath;
        }

        System.out.println("Path: " + path);
        fileBrowser.getHolder().executor.submit(() -> {
            try {
                this.fs = new LocalFileSystem();
                //Validate if local path exists, if not set the home path
                if (this.path == null || Files.notExists(Paths.get(this.path)) || !Files.isDirectory(Paths.get(this.path))) {
                    System.err.println("The file path doesn't exists " + this.path);
                    System.out.println("Setting to " + fs.getHome());

                    path = fs.getHome();
                }
                List<FileInfo> list = fs.list(path);
                SwingUtilities.invokeLater(() -> {
                    addressBar.setText(path);
                    folderView.setItems(list);
                    setTabTitle(path);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public String getTitlePrefix() {
        return getBundle().getString("app.files.tabs.local.title_prefix");
    }
    
    public void createAddressBar() {
        addressBar = new AddressBar(File.separatorChar, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedPath = e.getActionCommand();
                addressPopup.setName(selectedPath);
                MouseEvent me = (MouseEvent) e.getSource();
                addressPopup.show(me.getComponent(), me.getX(), me.getY());
                System.out.println("clicked");
            }
        });
        if (SettingsService.getSettings().isShowPathBar()) {
            addressBar.switchToPathBar();
        } else {
            addressBar.switchToText();
        }
    }

    @Override
    public String toString() {
        return "Local files [" + this.path + "]";
    }

    public String getHostText() {
        return "Local files";
    }

    public String getPathText() {
        return (this.path == null || this.path.length() < 1 ? "" : this.path);
    }

    @Override
    public void render(String path, boolean useCache) {
        this.render(path);
    }

    @Override
    public void render(String path) {
        this.path = path;
        fileBrowser.getHolder().submitLocalOperation(() -> {
            if (this.path == null) {
                this.path = fs.getHome();
            }
            List<FileInfo> list = fs.list(this.path);
            SwingUtilities.invokeLater(() -> {
                addressBar.setText(this.path);
                folderView.setItems(list);
                int tc = list.size();
                String text = String.format("Total %d remote file(s)", tc);
                fileBrowser.updateRemoteStatus(text);
                setTabTitle(this.path);
            });
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (this.path == null) {
//                    this.path = fs.getHome();
//                }
//                List<FileInfo> list = fs.list(this.path);
//                SwingUtilities.invokeLater(() -> {
//                    addressBar.setText(this.path);
//                    folderView.setItems(list);
//                    int tc = list.size();
//                    String text = String.format("Total %d remote file(s)", tc);
//                    fileBrowser.updateRemoteStatus(text);
//                    tabTitle.getCallback().accept(PathUtils.getFileName(this.path));
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            fileBrowser.enableUi();
//        });
    }

    @Override
    public void openApp(FileInfo file) {
    }

    @Override
    public boolean createMenu(JPopupMenu popup, FileInfo[] files) {
        menuHandler.createMenu(popup, files);
        return true;
    }

    protected void up() {
        String s = new File(path).getParent();
        if (s != null) {
            addBack(path);
            render(s);
        }
    }

    protected void home() {
        addBack(path);
        render(null);
    }

    @Override
    public void install(JComponent c) {

    }

    public boolean handleDrop(DndTransferData transferData) {
        System.out.println("### " + transferData.getDestination() + " " + this.hashCode());
        if (transferData.getDestination() == this) {
            return false; // TODO handle move files inside same browser
        }
        return this.fileBrowser.handleLocalDrop(transferData, this.fs, this.path);
    }

    public FileSystem getFileSystem() {
        return new LocalFileSystem();
    }
}
