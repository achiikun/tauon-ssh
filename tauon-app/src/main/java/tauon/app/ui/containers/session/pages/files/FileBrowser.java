package tauon.app.ui.containers.session.pages.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsService;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel;
import tauon.app.ui.components.misc.SkinnedSplitPane;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.containers.main.FileTransferProgress;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.files.local.LocalFileBrowserView;
import tauon.app.ui.containers.session.pages.files.ssh.SshFileBrowserView;
import tauon.app.ui.containers.session.pages.files.transfer.FileTransfer;
import tauon.app.ui.containers.session.pages.files.transfer.DndTransferData;
import tauon.app.util.misc.Constants;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static tauon.app.services.LanguageService.getBundle;

public class FileBrowser extends Page {
    private static final Logger LOG = LoggerFactory.getLogger(FileBrowser.class);
    
    private final JSplitPane horizontalSplitter;
    private final ClosableTabbedPanel leftTabs;
    private final ClosableTabbedPanel rightTabs;
    private final SessionContentPanel holder;
    private final SessionInfo info;
    private final Map<String, List<FileInfo>> sshDirCache = new HashMap<>();
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final JPopupMenu popup;
    private final Map<UUID, AbstractFileBrowserView> viewList = new HashMap<>();
    private FileTransfer ongoingFileTransfer;
    private boolean leftPopup = false;
    private JLabel lblStat1;
    private Box statusBox;
    
    private ThreadPoolExecutor backgroundTransferPool;

    public FileBrowser(SessionInfo info, SessionContentPanel holder) {

        this.info = info;
        this.holder = holder;

        JMenuItem localMenuItem = new JMenuItem("Local file browser");
        JMenuItem remoteMenuItem = new JMenuItem("Remote file browser");

        popup = new JPopupMenu();
        popup.add(remoteMenuItem);
        popup.add(localMenuItem);
        popup.pack();

        localMenuItem.addActionListener(e -> {
            if (leftPopup) {
                openLocalFileBrowserView(null, AbstractFileBrowserView.PanelOrientation.LEFT);
            } else {
                openLocalFileBrowserView(null, AbstractFileBrowserView.PanelOrientation.RIGHT);
            }
        });

        remoteMenuItem.addActionListener(e -> {
            if (leftPopup) {
                openSshFileBrowserView(null, AbstractFileBrowserView.PanelOrientation.LEFT);
            } else {
                openSshFileBrowserView(null, AbstractFileBrowserView.PanelOrientation.RIGHT);
            }
        });

        this.leftTabs = new ClosableTabbedPanel(c -> {
            popup.setInvoker(c);
            leftPopup = true;
            popup.show(c, 0, c.getHeight());
        });

        this.rightTabs = new ClosableTabbedPanel(c -> {
            popup.setInvoker(c);
            leftPopup = false;
            popup.show(c, 0, c.getHeight());
        });

        horizontalSplitter = new SkinnedSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontalSplitter.setResizeWeight(0.5);
        horizontalSplitter.setLeftComponent(this.leftTabs);
        horizontalSplitter.setRightComponent(this.rightTabs);
        horizontalSplitter.setDividerSize(5);

        if (SettingsService.getSettings().isDualPaneMode()) {
            switchToDualPaneMode();
        } else {
            switchToSinglePanelMode();
        }

    }


    private void switchToDualPaneMode() {
        horizontalSplitter.setRightComponent(this.rightTabs);
        horizontalSplitter.setLeftComponent(this.leftTabs);
        this.add(horizontalSplitter);
        this.revalidate();
        this.repaint();
    }

    private void switchToSinglePanelMode() {
        this.remove(horizontalSplitter);
        this.add(this.leftTabs);
        this.revalidate();
        this.repaint();
    }

//    public void disableUi() {
//        holder.disableUi();
//    }
//
//    public void disableUi(AtomicBoolean stopFlag) {
//        holder.disableUi(stopFlag);
//    }
//
//    public void enableUi() {
//        holder.enableUi();
//    }

    public void openSshFileBrowserView(String path, AbstractFileBrowserView.PanelOrientation orientation) {
        SshFileBrowserView tab = new SshFileBrowserView(this, path, orientation);
        if (orientation == AbstractFileBrowserView.PanelOrientation.LEFT) {
            this.leftTabs.addTab(tab.getTabTitle(), tab);
        } else {
            this.rightTabs.addTab(tab.getTabTitle(), tab);
        }
    }

    public void openLocalFileBrowserView(String path, AbstractFileBrowserView.PanelOrientation orientation) {

        LocalFileBrowserView tab = new LocalFileBrowserView(this, path, orientation);
        if (orientation == AbstractFileBrowserView.PanelOrientation.LEFT) {
            this.leftTabs.addTab(tab.getTabTitle(), tab);
        } else {
            this.rightTabs.addTab(tab.getTabTitle(), tab);
        }
    }

    public SshFileSystem getSSHFileSystem() {
        return this.getHolder().getSshFs();
    }

//    public TauonRemoteSessionInstance getSessionInstance() {
//        return this.holder.getRemoteSessionInstance();
//    }

    public SessionInfo getInfo() {
        return info;
    }

    public Map<String, List<FileInfo>> getSSHDirectoryCache() {
        return this.sshDirCache;
    }

    public void newFileTransfer(FileSystem sourceFs, FileSystem targetFs, FileInfo[] files, String targetFolder) {
        System.out.println("Initiating new file transfer...");
//        this.ongoingFileTransfer =
        
        holder.getAppWindow().getFileTransferManager().startFileTransfer(
                new FileTransfer(sourceFs, targetFs, files, targetFolder, SettingsService.getSettings().getConflictAction(), holder),
                false,
                new FileTransferProgress.Adapter(){

                    @Override
                    public void error(String cause, FileTransfer fileTransfer) {
                        if (!holder.isSessionClosed()) {
                            JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                        }
                    }
        
                    @Override
                    public void done(FileTransfer fileTransfer) {
                        reloadView();
                    }
                }
        );
        
    }

    private void reloadView() {
        Component c = leftTabs.getSelectedContent();
        System.out.println("c1 " + c);
        if (c instanceof AbstractFileBrowserView) {
            ((AbstractFileBrowserView) c).reload();
        }
        c = rightTabs.getSelectedContent();
        System.out.println("c2 " + c);
        if (c instanceof AbstractFileBrowserView) {
            ((AbstractFileBrowserView) c).reload();
        }
    }
    
    @Override
    public void onLoad() {
        if (init.get()) {
            return;
        }
        init.set(true);
        SshFileBrowserView left = new SshFileBrowserView(this, null, AbstractFileBrowserView.PanelOrientation.LEFT);
        this.leftTabs.addTab(left.getTabTitle(), left);

        LocalFileBrowserView right = new LocalFileBrowserView(this, System.getProperty("user.home"),
                AbstractFileBrowserView.PanelOrientation.RIGHT);
        this.rightTabs.addTab(right.getTabTitle(), right);
    }

    @Override
    public String getIcon() {
        return FontAwesomeContants.FA_FOLDER;
    }

    @Override
    public String getText() {
        return getBundle().getString("app.files.title");
    }

    /**
     * @return the holder
     */
    public SessionContentPanel getHolder() {
        return holder;
    }

    public void openPath(String path) {
        openSshFileBrowserView(path, AbstractFileBrowserView.PanelOrientation.LEFT);
    }

    public boolean isSessionClosed() {
        return this.holder.isSessionClosed();
    }

    public boolean handleLocalDrop(DndTransferData transferData, FileSystem currentFileSystem,
                                   String currentPath) {
        // TODO i18n
        if (SettingsService.getSettings().isConfirmBeforeMoveOrCopy()
                && JOptionPane.showConfirmDialog(null, "Move/copy files?") != JOptionPane.YES_OPTION) {
            return false;
        }

        try {

            System.out.println("Dropped: " + transferData);
            AbstractFileBrowserView source = transferData.getSource();
            if (source == null) {
                // Comes from local
                System.out.println("Session hash code: " + source);
                return true;
            }

            if (source.getFileBrowser() == this) {
                if (SettingsService.getSettings().getFileTransferMode() == Constants.TransferMode.BACKGROUND) {
                    downloadInBackground(transferData.getFiles(), currentPath);
                    return true;
                }
                FileSystem sourceFs = this.getSSHFileSystem();
                if (sourceFs == null) {
                    return false;
                }
                FileSystem targetFs = currentFileSystem;
                this.newFileTransfer(sourceFs, targetFs, transferData.getFiles(), currentPath);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void downloadInBackground(FileInfo[] remoteFiles, String targetLocalDirectory) {
        FileSystem targetFs = new LocalFileSystem();
        TauonRemoteSessionInstance instance = getHolder().createBackgroundSession();
        SshFileSystem sourceFs = instance.getSshFs();
        
        FileTransfer transfer = new FileTransfer(sourceFs, targetFs, remoteFiles, targetLocalDirectory,
                SettingsService.getSettings().getConflictAction(),
                getHolder());
        getHolder().getAppWindow().getFileTransferManager().startFileTransfer(
                transfer,
                true,
                new FileTransferProgress.Adapter(){
                    @Override
                    public void done(FileTransfer fileTransfer) {
                        getHolder().returnToSessionCache(instance);
                    }
                    
                    @Override
                    public void error(String cause, FileTransfer fileTransfer) {
                        getHolder().returnToSessionCache(instance);
                    }
                }
        );
    }
    
    public void uploadInBackground(FileInfo[] localFiles, String targetRemoteDirectory) {
        TauonRemoteSessionInstance instance = getHolder().createBackgroundSession();
        FileSystem sourceFs = new LocalFileSystem();
        FileSystem targetFs = instance.getSshFs();
        FileTransfer transfer = new FileTransfer(sourceFs, targetFs, localFiles, targetRemoteDirectory,
                SettingsService.getSettings().getConflictAction(),
                getHolder());
        getHolder().getAppWindow().getFileTransferManager().startFileTransfer(
                transfer,
                true,
                new FileTransferProgress.Adapter(){
                    @Override
                    public void done(FileTransfer fileTransfer) {
                        getHolder().returnToSessionCache(instance);
                    }
                    
                    @Override
                    public void error(String cause, FileTransfer fileTransfer) {
                        getHolder().returnToSessionCache(instance);
                    }
                }
        );
        
    }

    public void refreshViewMode() {
        for (AbstractFileBrowserView view : this.viewList.values()) {
            view.refreshViewMode();
        }
        this.revalidate();
        this.repaint(0);
    }

    public void registerForViewNotification(AbstractFileBrowserView view) {
        this.viewList.put(view.getUUID(), view);
    }

    public void unRegisterForViewNotification(AbstractFileBrowserView view) {
        this.viewList.remove(view.getUUID());
    }

    public void updateRemoteStatus(String text) {
    }
    
    public void notifyTransferDone(FileTransfer fileTransfer) {
        reloadView();
    }
    
    public synchronized ThreadPoolExecutor getBackgroundTransferPool() {
        if (this.backgroundTransferPool == null) {
            this.backgroundTransferPool = new ThreadPoolExecutor(
                    SettingsService.getSettings().getBackgroundTransferQueueSize(),
                    SettingsService.getSettings().getBackgroundTransferQueueSize(), 0, TimeUnit.NANOSECONDS,
                    new LinkedBlockingQueue<>());
        } else {
            if (this.backgroundTransferPool.getMaximumPoolSize() != SettingsService.getSettings()
                    .getBackgroundTransferQueueSize()) {
                this.backgroundTransferPool
                        .setMaximumPoolSize(SettingsService.getSettings().getBackgroundTransferQueueSize());
            }
        }
        return this.backgroundTransferPool;
    }
    
    public void close() {
        
        if (this.backgroundTransferPool != null) {
            this.backgroundTransferPool.shutdownNow();
            
            try {
                if(!this.backgroundTransferPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)){
                    LOG.error("the background transfer pool was not fully shutdown");
                }
            } catch (InterruptedException e1) {
                LOG.error("Error while closing the background transfer pool", e1);
            }
        }
        
    }
    
    public AbstractFileBrowserView findViewById(UUID uuid) {
        return viewList.get(uuid);
    }
    
//    public static class ResponseHolder {
//        public Constants.TransferMode transferMode;
//        public Constants.ConflictAction conflictAction;
//    }
}
