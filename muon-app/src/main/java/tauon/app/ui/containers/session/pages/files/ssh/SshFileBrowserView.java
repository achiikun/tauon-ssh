package tauon.app.ui.containers.session.pages.files.ssh;

import tauon.app.App;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ssh.OperationCancelledException;
import tauon.app.ui.containers.session.pages.files.AbstractFileBrowserView;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.view.AddressBar;
import tauon.app.ui.containers.session.pages.files.view.DndTransferData;
import tauon.app.ui.containers.session.pages.files.view.DndTransferHandler;
import tauon.app.ssh.filesystem.SshFileSystem;
import util.Constants;
import util.PathUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SshFileBrowserView extends AbstractFileBrowserView {
    private final SshMenuHandler menuHandler;
    private final JPopupMenu addressPopup;
    private final DndTransferHandler transferHandler;

    public SshFileBrowserView(FileBrowser fileBrowser, String initialPath, PanelOrientation orientation) {
        super(orientation, fileBrowser);
        this.menuHandler = new SshMenuHandler(fileBrowser, this);
        this.menuHandler.initMenuHandler(this.folderView);
        this.transferHandler = new DndTransferHandler(this.folderView, this.fileBrowser.getInfo(), this,
                DndTransferData.DndSourceType.SSH, this.fileBrowser);
        this.folderView.setTransferHandler(transferHandler);
        this.folderView.setFolderViewTransferHandler(transferHandler);
        this.addressPopup = menuHandler.createAddressPopup();
        if (initialPath == null) {
            this.path = this.fileBrowser.getInfo().getRemoteFolder();
            if (this.path != null && this.path.trim().length() < 1) {
                this.path = null;
            }
            System.out.println("Path: " + path);
        } else {
            this.path = initialPath;
        }

        this.render(path, App.getGlobalSettings().isDirectoryCache());
    }

    private void openDefaultAction() {
    }

    private void openNewTab() {

    }

    public void createAddressBar() {
        addressBar = new AddressBar('/', new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedPath = e.getActionCommand();
                addressPopup.setName(selectedPath);
                MouseEvent me = (MouseEvent) e.getSource();
                addressPopup.show(me.getComponent(), me.getX(), me.getY());
                System.out.println("clicked");
            }
        });
        if (App.getGlobalSettings().isShowPathBar()) {
            addressBar.switchToPathBar();
        } else {
            addressBar.switchToText();
        }
    }

    @Override
    public String toString() {
        return this.fileBrowser.getInfo().getName()
                + (this.path == null || this.path.length() < 1 ? "" : " [" + this.path + "]");
    }

    private String trimPath(String path) {
        if (path.equals("/"))
            return path;
        if (path.endsWith("/")) {
            String trim = path.substring(0, path.length() - 1);
            System.out.println("Trimmed path: " + trim);
            return trim;
        }
        return path;
    }

    private void renderDirectory(final String path, final boolean fromCache) throws Exception {
        List<FileInfo> list = null;
        if (fromCache) {
            list = this.fileBrowser.getSSHDirectoryCache().get(trimPath(path));
        }
        if (list == null) {
            list = this.fileBrowser.getSSHFileSystem().list(path);
            if (list != null) {
                this.fileBrowser.getSSHDirectoryCache().put(trimPath(path), list);
            }
        }
        if (list != null) {
            final List<FileInfo> list2 = list;
            System.out.println("New file list: " + list2);
            SwingUtilities.invokeLater(() -> {
                addressBar.setText(path);
                folderView.setItems(list2);
                tabTitle.getCallback().accept(PathUtils.getFileName(path));
                int tc = list2.size();
                String text = String.format("Total %d remote file(s)", tc);
                fileBrowser.updateRemoteStatus(text);
            });
        }
    }

    @Override
    public void render(String path, boolean useCache) {
        System.out.println("Rendering: " + path + " caching: " + useCache);
        this.path = path;
        fileBrowser.getHolder().executor.submit(() -> {
            this.fileBrowser.disableUi();
            try {
                while (!fileBrowser.isCloseRequested()) {
                    System.out.println("Listing files now ...");
                    try {
                        if (path == null) {
                            SshFileSystem sshfs = this.fileBrowser.getSSHFileSystem();
                            this.path = sshfs.getHome();
                        }
                        renderDirectory(this.path, useCache);
                        break;
                    } catch (OperationCancelledException e) {
                        e.printStackTrace();

                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (this.fileBrowser.isSessionClosed()) {
                            return;
                        }
                        System.out.println("Exception caught in sftp file browser: " + e.getMessage());

                        // TODO i18n
                        if (JOptionPane.showConfirmDialog(null,
                                "Unable to connect to server " + this.fileBrowser.getInfo().getName() + " at "
                                        + this.fileBrowser.getInfo().getHost()
                                        + (e.getMessage() != null ? "\n\nReason: " + e.getMessage() : "\n")
                                        + "\n\nDo you want to retry?") == JOptionPane.YES_OPTION) {
                            
                            this.fileBrowser.getHolder().reconnect();
                            
                            continue;
                        }
                        break;
                    }
                }
            } finally {
                this.fileBrowser.enableUi();
            }
        });
    }

    @Override
    public void render(String path) {
        this.render(path, false);
    }

    @Override
    public void openApp(FileInfo file) {

        FileInfo fileInfo = folderView.getSelectedFiles()[0];
        try {
            App.getExternalEditorHandler().openRemoteFile(fileInfo, fileBrowser.getSSHFileSystem(),
                    fileBrowser.getActiveSessionId(), false, null);
        } catch (IOException e1) {
            // TODO handle exception
            e1.printStackTrace();
        }

    }

    protected void up() {
        if (path != null) {
            String parent = PathUtils.getParent(path);
            addBack(path);
            render(parent, App.getGlobalSettings().isDirectoryCache());
        }
    }

    protected void home() {
        addBack(path);
        render(null, App.getGlobalSettings().isDirectoryCache());
    }

    @Override
    public void install(JComponent c) {

    }

    @Override
    public boolean createMenu(JPopupMenu popup, FileInfo[] files) {
        if (this.path == null) {
            return false;
        }
        return menuHandler.createMenu(popup, files);
    }

    public boolean handleDrop(DndTransferData transferData) {
        if (App.getGlobalSettings().isConfirmBeforeMoveOrCopy()
                && JOptionPane.showConfirmDialog(null, "Move/copy files?") != JOptionPane.YES_OPTION) {
            return false;
        }
        try {
            int sessionHashCode = transferData.getInfo();
            System.out.println("Session hash code: " + sessionHashCode);
            FileSystem sourceFs = null;
            if (sessionHashCode == 0 && transferData.getSourceType() == DndTransferData.DndSourceType.LOCAL) {
                System.out.println("Source fs is local");
                sourceFs = new LocalFileSystem();
            } else if (transferData.getSourceType() == DndTransferData.DndSourceType.SSH
                    && sessionHashCode == this.fileBrowser.getInfo().hashCode()) {
                System.out.println("Source fs is remote");
                sourceFs = this.fileBrowser.getSSHFileSystem();
            }

            if (sourceFs instanceof LocalFileSystem) {
                System.out.println("Dropped: " + transferData);
                FileBrowser.ResponseHolder holder = new FileBrowser.ResponseHolder();
                if (!this.fileBrowser.selectTransferModeAndConflictAction(holder)) {
                    return false;
                }
                if (holder.transferMode == Constants.TransferMode.BACKGROUND) {
                    this.fileBrowser.getHolder().uploadInBackground(transferData.getFiles(), this.path,
                            holder.conflictAction);
                    return true;
                }
                FileSystem targetFs = this.fileBrowser.getSSHFileSystem();
                this.fileBrowser.newFileTransfer(sourceFs, targetFs, transferData.getFiles(), this.path,
                        this.hashCode(), holder.conflictAction, null);
            } else if (sourceFs instanceof SshFileSystem && (sourceFs == this.fileBrowser.getSSHFileSystem())) {
                System.out.println("SshFs is of same instance: " + (sourceFs == this.fileBrowser.getSSHFileSystem()));
                if (transferData.getFiles().length > 0) {
                    FileInfo fileInfo = transferData.getFiles()[0];
                    String parent = PathUtils.getParent(fileInfo.getPath());
                    System.out.println("Parent: " + parent + " == " + this.getCurrentDirectory());
                    if (!parent.endsWith("/")) {
                        parent += "/";
                    }
                    String pwd = this.getCurrentDirectory();
                    if (!pwd.endsWith("/")) {
                        pwd += "/";
                    }
                    if (parent.equals(pwd)) {
                        JOptionPane.showMessageDialog(null, "Source and target directory is same!");
                        return false;
                    }
                }

                if (transferData.getTransferAction() == DndTransferData.TransferAction.Copy) {
                    menuHandler.copy(Arrays.asList(transferData.getFiles()), getCurrentDirectory());
                } else {
                    menuHandler.move(Arrays.asList(transferData.getFiles()), getCurrentDirectory());
                }
            } else if (sourceFs instanceof SshFileSystem
                    && (transferData.getSourceType() == DndTransferData.DndSourceType.SFTP)) {
            }
            System.out.println("12345: " + (sourceFs instanceof SshFileSystem) + " " + transferData.getSourceType());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public FileSystem getFileSystem() throws Exception {
        return this.fileBrowser.getSSHFileSystem();
    }

    public TauonRemoteSessionInstance getSshClient() throws Exception {
        return this.fileBrowser.getSessionInstance();
    }

    @Override
    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    public String getHostText() {
        return this.fileBrowser.getInfo().getName();
    }

    public String getPathText() {
        return (this.path == null || this.path.length() < 1 ? "" : this.path);
    }

}