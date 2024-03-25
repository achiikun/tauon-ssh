/**
 *
 */
package tauon.app.ui.containers.session;

import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.SessionService;
import tauon.app.services.SettingsService;
import tauon.app.ui.containers.session.pages.diskspace.DiskspaceAnalyzer;
import net.schmizz.sshj.connection.channel.direct.Session;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.transfer.BackgroundFileTransfer;
import tauon.app.ui.containers.session.pages.logviewer.LogViewer;
import tauon.app.ui.containers.session.pages.processview.ProcessViewer;
import tauon.app.ui.containers.session.pages.search.SearchPanel;
import tauon.app.ui.containers.session.pages.terminal.TerminalHolder;
import tauon.app.ui.containers.session.pages.utilpage.UtilityPage;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import tauon.app.App;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ui.components.glasspanes.ProgressGlasspane;
import tauon.app.ui.components.glasspanes.SessionInputBlocker;
import tauon.app.ui.components.misc.TabbedPage;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.components.page.PageHolder;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.containers.main.FileTransferProgress;
import tauon.app.settings.HopEntry;
import tauon.app.ui.dialogs.sessions.PasswordPromptHelper;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SessionContentPanel extends JPanel implements PageHolder, GuiHandle<TauonRemoteSessionInstance> {
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SessionInfo info;
    private final AppWindow appWindow;
    
    
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JRootPane rootPane;
    private final JPanel contentPane;
    private final SessionInputBlocker sessionInputBlocker = new SessionInputBlocker();
    
    private final ProgressGlasspane progressPanel = new ProgressGlasspane();
    
    private final TabbedPage[] pages;
    private final FileBrowser fileBrowser;
    private final LogViewer logViewer;
    private final TerminalHolder terminalHolder;
    private final DiskspaceAnalyzer diskspaceAnalyzer;
    private final SearchPanel searchPanel;
    private final ProcessViewer processViewer;
    private final UtilityPage utilityPage;
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private final Deque<TauonRemoteSessionInstance> cachedSessions = new LinkedList<>();
    private TauonRemoteSessionInstance remoteSessionInstance;
    
    private ThreadPoolExecutor backgroundTransferPool;
    
    /**
     *
     */
    public SessionContentPanel(SessionInfo info, AppWindow appWindow) {
        super(new BorderLayout());
        
        this.info = info;
        this.appWindow = appWindow;
        
        this.remoteSessionInstance = new TauonRemoteSessionInstance(
                info,
                this,
                new MyPasswordFinder(),
                true);
        
        Box contentTabs = Box.createHorizontalBox();
        contentTabs.setBorder(new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()));

        fileBrowser = new FileBrowser(info, this, null, this.hashCode());
        logViewer = new LogViewer(this);
        terminalHolder = new TerminalHolder(info, this);
        diskspaceAnalyzer = new DiskspaceAnalyzer(this);
        searchPanel = new SearchPanel(this);
        processViewer = new ProcessViewer(this);
        utilityPage = new UtilityPage(this);

        Page[] pageArr = null;
        if (SettingsService.getSettings().isFirstFileBrowserView()) {
            pageArr = new Page[]{fileBrowser, terminalHolder, logViewer, searchPanel, diskspaceAnalyzer,
                    processViewer, utilityPage};
        } else {
            pageArr = new Page[]{terminalHolder, fileBrowser, logViewer, searchPanel, diskspaceAnalyzer,
                    processViewer, utilityPage};
        }

        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(this.cardLayout);

        this.pages = new TabbedPage[pageArr.length];
        for (int i = 0; i < pageArr.length; i++) {
            TabbedPage tabbedPage = new TabbedPage(pageArr[i], this);
            this.pages[i] = tabbedPage;
            this.cardPanel.add(tabbedPage.getPage(), tabbedPage.getId());
            pageArr[i].putClientProperty("pageId", tabbedPage.getId());
        }

        LayoutUtilities.equalizeSize(this.pages);

        for (TabbedPage item : this.pages) {
            contentTabs.add(item);
        }

        contentTabs.add(Box.createHorizontalGlue());

        this.contentPane = new JPanel(new BorderLayout(), true);
        this.contentPane.add(contentTabs, BorderLayout.NORTH);
        this.contentPane.add(this.cardPanel);

        this.rootPane = new JRootPane();
        this.rootPane.setContentPane(this.contentPane);

        this.add(this.rootPane);

        showPage(this.pages[0].getId());

    }

    public void reconnect() {
        
        // If the main remote session is reconnected (the cached sessions must be also, because the failure
        // has been produced in the connection itself)
        try {
            this.remoteSessionInstance.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.cachedSessions.forEach((r) -> {
                try {
                    r.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e); // TODO
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        cachedSessions.clear();
        
        try {
            this.remoteSessionInstance.close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        
        this.remoteSessionInstance = new TauonRemoteSessionInstance(info, this, new MyPasswordFinder(), true);
        
        try {
            this.remoteSessionInstance.connect();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
    }

    @Override
    public void showPage(String pageId) {
        TabbedPage selectedPage = null;
        for (TabbedPage item : this.pages) {
            if (pageId.equals(item.getId())) {
                selectedPage = item;
            }
            item.setSelected(false);
        }
        selectedPage.setSelected(true);
        this.cardLayout.show(this.cardPanel, pageId);
        this.revalidate();
        this.repaint();
        selectedPage.getPage().onLoad();
    }

    /**
     * @return the info
     */
    public SessionInfo getInfo() {
        return info;
    }

    /**
     * @return the remoteSessionInstance
     */
    public TauonRemoteSessionInstance getRemoteSessionInstance() {
        return remoteSessionInstance;
    }

    public void disableUi() {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.startAnimation(null);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            System.out.println("Showing disable panel");
            this.sessionInputBlocker.setVisible(true);
        });
    }

    public void disableUi(AtomicBoolean stopFlag) {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.startAnimation(stopFlag);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            System.out.println("Showing disable panel");
            this.sessionInputBlocker.setVisible(true);
        });
    }

    public void enableUi() {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.stopAnimation();
            System.out.println("Hiding disable panel");
            this.sessionInputBlocker.setVisible(false);
        });
    }

    public void startFileTransferModal(Consumer<Boolean> stopCallback) {
        progressPanel.setStopCallback(stopCallback);
        progressPanel.clear();
        this.rootPane.setGlassPane(this.progressPanel);
        progressPanel.setVisible(true);
        this.revalidate();
        this.repaint();
    }

    public void setTransferProgress(int progress) {
        progressPanel.setProgress(progress);
    }

    public void endFileTransfer() {
        progressPanel.setVisible(false);
        this.revalidate();
        this.repaint();
    }

    public int getActiveSessionId() {
        return this.hashCode();
    }

    public void downloadFileToLocal(FileInfo remoteFile, Consumer<File> callback) {

    }

    public void openLog(FileInfo remoteFile) {
        showPage(this.logViewer.getClientProperty("pageId") + "");
        logViewer.openLog(remoteFile);
    }

    public void openFileInBrowser(String path) {
        showPage(this.fileBrowser.getClientProperty("pageId") + "");
        fileBrowser.openPath(path);
    }

    public void openTerminal(String command) {
        showPage(this.terminalHolder.getClientProperty("pageId") + "");
        this.terminalHolder.openNewTerminal(command);
    }
    
    public Session openSession() throws Exception {
        return remoteSessionInstance.openSession();
    }
    
    /**
     * @return the closed
     */
    public boolean isSessionClosed() {
        return closed.get();
    }

    public void close() {
        if(closed.getAndSet(true))
            return;
        
        try {
            this.terminalHolder.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (this.backgroundTransferPool != null) {
            this.backgroundTransferPool.shutdownNow();
            
            try {
                this.backgroundTransferPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        
        try {
            this.remoteSessionInstance.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.cachedSessions.forEach(c -> {
                try {
                    c.close();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e); // TODO
                }
            });
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        
        this.executor.shutdownNow();
        
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        appWindow.removePendingTransfers(this.getActiveSessionId());
        
    }

    public void uploadInBackground(FileInfo[] localFiles, String targetRemoteDirectory, Constants.ConflictAction confiAction) {
        TauonRemoteSessionInstance instance = createBackgroundSession();
        FileSystem sourceFs = new LocalFileSystem();
        FileSystem targetFs = instance.getSshFs();
        BackgroundFileTransfer transfer = new BackgroundFileTransfer(sourceFs, targetFs, localFiles, targetRemoteDirectory, null,
                confiAction, instance, this);
        FileTransferProgress p = appWindow.addUpload(transfer);
        getBackgroundTransferPool().submit(() -> {
            try {
                transfer.run(p);
            } catch (Exception e) {
                e.printStackTrace();
                p.error(e.getMessage(), transfer);
            }finally {
                returnToSessionCache(instance);
            }
        });
    }

    public void downloadInBackground(FileInfo[] remoteFiles, String targetLocalDirectory, Constants.ConflictAction confiAction) {
        FileSystem targetFs = new LocalFileSystem();
        TauonRemoteSessionInstance instance = createBackgroundSession();
        SshFileSystem sourceFs = instance.getSshFs();
        BackgroundFileTransfer transfer = new BackgroundFileTransfer(sourceFs, targetFs, remoteFiles, targetLocalDirectory, null,
                confiAction, instance, this);
        FileTransferProgress p = appWindow.addDownload(transfer);
        getBackgroundTransferPool().submit(() -> {
            try {
                transfer.run(p);
            } catch (Exception e) {
                e.printStackTrace();
                p.error(e.getMessage(), transfer);
            }finally {
                returnToSessionCache(instance);
            }
        });
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

    public synchronized TauonRemoteSessionInstance createBackgroundSession() {
        if (this.cachedSessions.isEmpty()) {
            return new TauonRemoteSessionInstance(info, this, new MyPasswordFinder(), false);
        }
        return this.cachedSessions.pop();
    }

    public synchronized void returnToSessionCache(TauonRemoteSessionInstance session) {
        if(!session.isSessionClosed())
            this.cachedSessions.push(session);
    }

    @Override
    public void reportException(Throwable cause) {
    
    }
    
    @Override
    public char[] promptPassword(HopEntry info, String user, AtomicBoolean rememberPassword, boolean isRetrying) {
        JPasswordField passwordField = new JPasswordField(30);
        int ret;
        if(rememberPassword != null) {
            JCheckBox rememberCheckBox = new JCheckBox(getBundle().getString("remember_password"));
            ret = JOptionPane.showOptionDialog(
                    null,
                    new Object[]{
                            "Type the password for user '" + user + "'",
                            passwordField,
                            rememberCheckBox
                    },
                    "Input", // TODO i18n
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
            rememberPassword.set(rememberCheckBox.isSelected());
        }else{
            ret = JOptionPane.showOptionDialog(
                    null,
                    new Object[]{
                            "Type the password for user '" + user + "'",
                            passwordField
                    },
                    "Input", // TODO i18n
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
        }
        
        if (ret == JOptionPane.OK_OPTION) {
            return passwordField.getPassword();
        }
        
        return null;
    }
    
    @Override
    public void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e) {
    
    }
    
    @Override
    public BlockHandle blockUi(TauonRemoteSessionInstance client, UserCancellable userCancellable) {
        if(client == this.remoteSessionInstance) {
            BlockHandle block = new BlockHandle() {
                @Override
                public void unblock() {
                    appWindow.getInputBlocker().unblockInput();
                }
            };
            appWindow.getInputBlocker().blockInput(() -> userCancellable.userCancelled(block));
            return block;
        }
        return () -> {}; // No block
    }
    
    @Override
    public String promptUser(HopEntry info, AtomicBoolean remember) {
        
        JTextField txtUser = new SkinnedTextField(30);
        JCheckBox chkCacheUser = new JCheckBox(getBundle().getString("remember_username"));
        // TODO i18n
        int ret = JOptionPane.showOptionDialog(null, new Object[]{"User name", txtUser, chkCacheUser}, getBundle().getString("user"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (ret == JOptionPane.OK_OPTION) {
            String user = txtUser.getText();
            if (chkCacheUser.isSelected()) {
                remember.set(true);
            }
            return user;
        }
        
        return null;
    }
    
    private class MyPasswordFinder implements PasswordFinder{
        
        @Override
        public char[] reqPassword(Resource<?> resource) {
            if(SwingUtilities.isEventDispatchThread()){
                return showReqPasswordDialog(resource);
            }else{
                AtomicReference<char[]> ret = new AtomicReference<>();
                try {
                    SwingUtilities.invokeAndWait(() -> ret.set(showReqPasswordDialog(resource)));
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return ret.get();
            }
        }
        
        private boolean retryPassword = true;
        
        public char[] showReqPasswordDialog(Resource<?> resource) {
            JPasswordField txtPass = new JPasswordField();
//        JCheckBox chkUseCache = new JCheckBox(getBundle().getString("remember_session"));
            
            // TODO i18n
            int ret = JOptionPane.showOptionDialog(SessionContentPanel.this,
                    new Object[]{resource!=null?resource.toString(): "Private key passphrase:", txtPass},//, chkUseCache},
                    "Passphrase", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (ret == JOptionPane.OK_OPTION) {
                return txtPass.getPassword();
            }else{
                retryPassword = false;
            }
            
            return null;
        }
        
        @Override
        public boolean shouldRetry(Resource<?> resource) {
            boolean old = retryPassword;
            retryPassword = true;
            return old;
        }
        
    }
    
    @Override
    public void showMessage(String name, String instruction) {
        JOptionPane.showMessageDialog(this, instruction, name, JOptionPane.PLAIN_MESSAGE);
    }
    
    @Override
    public String promptInput(String prompt, boolean echo) {
        if(echo) {
            return JOptionPane.showInputDialog(this, prompt);
        }else{
            JPasswordField passwordField = new JPasswordField(30);
            int ret = JOptionPane.showOptionDialog(
                    this,
                    new Object[]{prompt, passwordField},
                    "Input", // TODO i18n
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
            if (ret == JOptionPane.OK_OPTION) {
                return String.valueOf(passwordField.getPassword());
            }
            return null;
        }
    }
    
    @Override
    public void saveInfo(SessionInfo info) {
        SessionService.getInstance().setPasswordsFrom(info);
        try {
            SessionService.getInstance().save(new PasswordPromptHelper(this));
        } catch (OperationCancelledException ignored) {
        
        }
    }
}
