/**
 *
 */
package tauon.app.ui.containers.session;

import com.intellij.util.ui.UIUtil;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.SessionService;
import tauon.app.services.SettingsService;
import tauon.app.settings.HopEntry;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.components.glasspanes.ProgressGlasspane;
import tauon.app.ui.components.glasspanes.SessionInputBlocker;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.components.misc.TabbedPage;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.components.page.PageHolder;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.containers.main.FileTransferProgress;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.info.InfoPage;
import tauon.app.ui.containers.session.pages.logviewer.LogViewer;
import tauon.app.ui.containers.session.pages.terminal.TerminalHolder;
import tauon.app.ui.containers.session.pages.tools.ToolsPage;
import tauon.app.ui.dialogs.sessions.PasswordPromptHelper;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SessionContentPanel extends JPanel implements PageHolder, GuiHandle<TauonRemoteSessionInstance> {
    private static final Logger LOG = LoggerFactory.getLogger(SessionContentPanel.class);
    
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SessionInfo info;
    private final AppWindow appWindow;
    
    private final UUID uuid = UUID.randomUUID();
    
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
    private final InfoPage processViewer;
    private final ToolsPage toolsPage;
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private final Deque<TauonRemoteSessionInstance> cachedSessions = new LinkedList<>();
    private final TauonRemoteSessionInstance remoteSessionInstance;
    
    
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
                true,
                appWindow.hostKeyVerifier);
        
        Box contentTabs = Box.createHorizontalBox();
        contentTabs.setBorder(new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()));

        fileBrowser = new FileBrowser(info, this);
        logViewer = new LogViewer(this);
        terminalHolder = new TerminalHolder(info, this);
        processViewer = new InfoPage(this);
        toolsPage = new ToolsPage(this);

        Page[] pageArr = null;
        if (SettingsService.getSettings().isFirstFileBrowserView()) {
            pageArr = new Page[]{fileBrowser, terminalHolder, logViewer,
                    processViewer, toolsPage};
        } else {
            pageArr = new Page[]{terminalHolder, fileBrowser, logViewer,
                    processViewer, toolsPage};
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
    
    public AppWindow getAppWindow() {
        return appWindow;
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
        assert selectedPage != null : "Page Id not existing.";
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
//    public TauonRemoteSessionInstance getRemoteSessionInstance() {
//        return remoteSessionInstance;
//    }

    private void disableUi() {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.startAnimation(null);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            this.sessionInputBlocker.setVisible(true);
        });
    }
    
    private void disableUi(AtomicBoolean stopFlag) {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.startAnimation(stopFlag);
            this.rootPane.setGlassPane(this.sessionInputBlocker);
            this.sessionInputBlocker.setVisible(true);
        });
    }
    
    private void enableUi() {
        SwingUtilities.invokeLater(() -> {
            this.sessionInputBlocker.stopAnimation();
            this.sessionInputBlocker.setVisible(false);
        });
    }
    
    public FileBrowser getFileBrowser() {
        return fileBrowser;
    }
    
    public FileTransferProgress startFileTransferModal(Consumer<Boolean> stopCallback) {
        rootPane.setGlassPane(this.progressPanel);
        FileTransferProgress h = progressPanel.show(stopCallback);
        this.revalidate();
        this.repaint();
        return h;
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
        remoteSessionInstance.ensureConnected();
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
            LOG.error("Error while closing Terminal", e);
        }
        
        this.fileBrowser.close();
        
        try {
            this.remoteSessionInstance.close();
        } catch (Exception e) {
            LOG.error("Error while closing the main session instance");
        }
        try {
            this.cachedSessions.forEach(c -> {
                try {
                    c.close();
                } catch (InterruptedException | IOException e) {
                    LOG.error("Error while closing a cached session instance");
                }
            });
        } catch (Exception e2) {
            LOG.error("Error while closing the cached session instances");
        }
        
        this.executor.shutdownNow();
        
        try {
            if(!this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)){
                LOG.error("The background executor was not fully shutdown");
            }
        } catch (InterruptedException e1) {
            LOG.error("Error while closing the background executor pool", e1);
        }
        
    }

    public synchronized TauonRemoteSessionInstance createBackgroundSession() {
        if (this.cachedSessions.isEmpty()) {
            return new TauonRemoteSessionInstance(
                    info, this, new MyPasswordFinder(), false,
                    appWindow.hostKeyVerifier
            );
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
    public boolean promptReconnect(String name, String host) {
        // TODO i18n
        return JOptionPane.showConfirmDialog(appWindow,
                "Unable to connect to server " + name + " at "
                        + host
//                                    + (e.getMessage() != null ? "\n\nReason: " + e.getMessage() : "\n")
                        + "\n\nDo you want to retry?") != JOptionPane.YES_OPTION;
    }
    
    @Override
    public char[] promptPassword(HopEntry info, String user, AtomicBoolean rememberPassword, boolean isRetrying) {
        JPasswordField passwordField = new JPasswordField(30);
        int ret;
        if(rememberPassword != null) {
            JCheckBox rememberCheckBox = new JCheckBox(getBundle().getString("remember_password"));
            // TODO i18n
            ret = JOptionPane.showOptionDialog(
                    appWindow,
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
            // TODO i18n
            ret = JOptionPane.showOptionDialog(
                    appWindow,
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
    public BlockHandle blockUi(TauonRemoteSessionInstance client, UserCancelHandle userCancelHandle) {
        if(client == this.remoteSessionInstance) {
            BlockHandle block = () -> appWindow.getInputBlocker().unblockInput();
            appWindow.getInputBlocker().blockInput(() -> userCancelHandle.userCancelled(block));
            return block;
        }
        return () -> {}; // No block
    }
    
    @Override
    public String promptUser(HopEntry info, AtomicBoolean remember) {
        
        JTextField txtUser = new SkinnedTextField(30);
        JCheckBox chkCacheUser = new JCheckBox(getBundle().getString("remember_username"));
        // TODO i18n
        try {
            int ret = UIUtil.invokeAndWaitIfNeeded(() ->
                    JOptionPane.showOptionDialog(
                            appWindow,
                            new Object[]{"User name", txtUser, chkCacheUser},
                            getBundle().getString("app.connections.label.user"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            null
                    )
            );
            
            if (ret == JOptionPane.OK_OPTION) {
                String user = txtUser.getText();
                if (chkCacheUser.isSelected()) {
                    remember.set(true);
                }
                return user;
            }
            
            return null;
            
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public void runSSHOperation(SSHOperator consumer) throws Exception {
        remoteSessionInstance.ensureConnected();
        try {
            consumer.operate(remoteSessionInstance);
        }catch (OperationCancelledException | AlreadyFailedException ignored){
        
        }
    }
    
    public <R> R runSSHOperation(SSHOperatorRet<R> consumer, R defaultIfFailedOrCancelled) throws Exception {
        remoteSessionInstance.ensureConnected();
        try {
            return consumer.operate(remoteSessionInstance);
        }catch (OperationCancelledException | AlreadyFailedException ignored){
            return defaultIfFailedOrCancelled;
        }
    }
    
    public void submitSSHOperation(SSHOperator consumer) {
        executor.submit(() -> {
            try {
                remoteSessionInstance.ensureConnected();
                disableUi();
                try {
                    consumer.operate(remoteSessionInstance);
                } catch (OperationCancelledException | AlreadyFailedException ignored) {

                } catch (Exception e) {
                    LOG.error("Operation failed", e);
                } finally {
                    enableUi();
                }
            } catch (Exception e) {
                LOG.error("Connection failed", e);
            }
        });
    }
    
    public void submitSSHOperationStoppable(SSHOperator consumer, AtomicBoolean stopFlag) {
        executor.submit(() -> {
            try {
                remoteSessionInstance.ensureConnected();
                disableUi(stopFlag);
                try {
                    consumer.operate(remoteSessionInstance);
                } catch (OperationCancelledException | AlreadyFailedException ignored) {
                
                } catch (Exception e) {
                    LOG.error("Operation failed", e);
                } finally {
                    enableUi();
                }
            } catch (Exception e) {
                LOG.error("Connection failed", e);
            }
        });
    }
    
    public void submitLocalOperation(LocalOperator consumer) {
        executor.submit(() -> {
            disableUi();
            try {
                consumer.operate();
            } catch (Exception e) {
                LOG.error("Operation failed", e);
            } finally {
                enableUi();
            }
        });
    }
    
    public SshFileSystem getSshFs() {
        return remoteSessionInstance.getSshFs();
    }
    
    public String getSudoPassword() {
        // TODO assuming password is also sudo, if not, ask user for password
        return info.getPassword();
    }
    
    private class MyPasswordFinder implements PasswordFinder{
        
        @Override
        public char[] reqPassword(Resource<?> resource) {
            try {
                return UIUtil.invokeAndWaitIfNeeded(this::showReqPasswordDialog, resource);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
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
    
    public interface SSHOperator {
        void operate(TauonRemoteSessionInstance instance) throws Exception;
    }
    
    public interface SSHOperatorRet<R> {
        R operate(TauonRemoteSessionInstance instance) throws Exception;
    }
    
    public interface LocalOperator {
        void operate() throws Exception;
    }
}
