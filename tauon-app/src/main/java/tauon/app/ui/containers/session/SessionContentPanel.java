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
import tauon.app.exceptions.*;
import tauon.app.services.SessionService;
import tauon.app.services.SettingsService;
import tauon.app.settings.HopEntry;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.*;
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
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.FormatUtils;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
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
        try {
            return remoteSessionInstance.openSession();
        }catch (RemoteOperationException.NotConnected e){
            LOG.error("Open session assured that you are not connected. Force a new connection.", e);
            remoteSessionInstance.ensureConnected(true);
            return remoteSessionInstance.openSession();
        }
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
        return JOptionPane.showConfirmDialog(appWindow,
                FormatUtils.$$(
                        getBundle().getString("app.session.message.unable_to_connect_retry"),
                        Map.of(
                                "SERVER_NAME", name,
                                "SERVER_HOST", host
                        )
                )
        ) != JOptionPane.YES_OPTION;
    }
    
    @Override
    public char[] promptPassword(HopEntry info, String user, AtomicBoolean rememberPassword, boolean isRetrying) throws OperationCancelledException {
        JPasswordField passwordField = new JPasswordField(30);
        int ret;
        if(rememberPassword != null) {
            JCheckBox rememberCheckBox = new JCheckBox(getBundle().getString("app.session.action.remember_password"));
            ret = JOptionPane.showOptionDialog(
                    appWindow,
                    new Object[]{
                            FormatUtils.$$(
                                    getBundle().getString("app.session.type_password_for_user.message"),
                                    Map.of(
                                            "USER", user
                                    )
                            ),
                            passwordField,
                            rememberCheckBox
                    },
                    getBundle().getString("app.session.type_password_for_user.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            );
            rememberPassword.set(rememberCheckBox.isSelected());
        }else{
            ret = JOptionPane.showOptionDialog(
                    appWindow,
                    new Object[]{
                            FormatUtils.$$(
                                    getBundle().getString("app.session.type_password_for_user.message"),
                                    Map.of(
                                            "USER", user
                                    )
                            ),
                            passwordField
                    },
                    getBundle().getString("app.session.type_password_for_user.title"),
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
        
        throw new OperationCancelledException();
    }
    
    @Override
    public char[] getSUDOPassword(boolean isRetrying) throws OperationCancelledException {
        if(!isRetrying){
            // TODO set SUDO in a separate field
            return this.info.getPassword().toCharArray();
        }else{
            return promptPassword(null, "SUDO", null, true);
        }
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
        JCheckBox chkCacheUser = new JCheckBox(getBundle().getString("app.session.action.remember_username"));
        try {
            int ret = UIUtil.invokeAndWaitIfNeeded(() ->
                    JOptionPane.showOptionDialog(
                            appWindow,
                            new Object[]{getBundle().getString("app.ui.label.username"), txtUser, chkCacheUser},
                            getBundle().getString("app.ui.label.user"),
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
    
    public void runSSHOperation(ISSHOperator consumer) {
        
        boolean force = false;
        while (true) {
            try {
                remoteSessionInstance.ensureConnected(force);
                
                try {
                    try {
                        consumer.operate(remoteSessionInstance);
                    }catch (IOException e){
                        throw new RemoteOperationException.RealIOException(e);
                    }
                    return;
                } catch (SessionClosedException exception) {
                    // Report error
                    LOG.error("Session was closed", exception);
                } catch (RemoteOperationException.NotConnected | RemoteOperationException.RealIOException ignored) {
                    // Reconnect
                    // Don't return (while true will re-execute ensureConnected forcing connection
                    continue;
                } catch (TauonOperationException exception) {
                    LOG.error("Going to show the exception to the user.", exception);
                    AlertDialogUtils.showError(this, exception.getUserMessage());
                }
                
            } catch (OperationCancelledException | AlreadyFailedException | InterruptedException e) {
                // Do nothing
                return;
            } catch (SessionClosedException e) {
                LOG.error("Session was closed", e);
            }
            
            return;
        }
    }
    
    public <R> R runSSHOperation(ISSHOperatorRet<R> consumer, R defaultIfFailedOrCancelled) throws Exception {
        remoteSessionInstance.ensureConnected();
        try {
            try {
                return consumer.operate(remoteSessionInstance);
            }catch (IOException e){
                throw new RemoteOperationException.RealIOException(e);
            }
        }catch (OperationCancelledException | AlreadyFailedException ignored){
            return defaultIfFailedOrCancelled;
        }
    }
    
    public void submitSSHOperation(ISSHOperator consumer) {
        submitSSHOperationStoppable(consumer, null);
    }
    
    public void submitSSHOperationStoppable(ISSHOperator consumer, AtomicBoolean stopFlag) {
        executor.submit(() -> {
            
            boolean force = false;
            while (true) {
                try {
                    remoteSessionInstance.ensureConnected(force);
                    
                    force = true;
                    disableUi(stopFlag);
                    try {
                        try {
                            consumer.operate(remoteSessionInstance);
                        }catch (IOException e){
                            throw new RemoteOperationException.RealIOException(e);
                        }
                    } catch (SessionClosedException exception) {
                        // Report error
                        LOG.error("Session was closed", exception);
                    } catch (RemoteOperationException.NotConnected | RemoteOperationException.RealIOException ignored) {
                        // Reconnect
                        // Don't return (while true will re-execute ensureConnected forcing connection
                        continue;
                    } catch (TauonOperationException exception) {
                        LOG.error("Going to show the exception to the user.", exception);
                        AlertDialogUtils.showError(this, exception.getUserMessage());
                    } finally {
                        enableUi();
                    }
                    
                } catch (OperationCancelledException | AlreadyFailedException | InterruptedException e) {
                    // Do nothing
                } catch (SessionClosedException e) {
                    LOG.error("Session was closed", e);
                }
                
                return;
            }
//            try {
//                remoteSessionInstance.ensureConnected();
//                disableUi(stopFlag);
//                try {
//                    consumer.operate(remoteSessionInstance);
//                } catch (OperationCancelledException | AlreadyFailedException ignored) {
//
//                } catch (Exception e) {
//                    LOG.error("Operation failed", e);
//                } finally {
//                    enableUi();
//                }
//            } catch (Exception e) {
//                LOG.error("Connection failed", e);
//            }
        });
    }
    
    public void submitLocalOperation(ILocalOperator consumer) {
        executor.submit(() -> {
            disableUi();
            try {
                consumer.operate();
            } catch (OperationCancelledException | AlreadyFailedException | InterruptedException ignored) {
                // Do nothing
            } catch (TauonOperationException exception) {
                LOG.error("Going to show the exception to the user.", exception);
                AlertDialogUtils.showError(this, exception.getUserMessage());
            } finally {
                enableUi();
            }
//            disableUi();
//            try {
//                consumer.operate();
//            } catch (Exception e) {
//                LOG.error("Operation failed", e);
//            } finally {
//                enableUi();
//            }
        });
    }
    
    public SshFileSystem getSshFs() {
        return remoteSessionInstance.getSshFs();
    }
    
//    public String getSudoPassword() {
//        // TODO assuming password is also sudo, if not, ask user for password
//        return info.getPassword();
//    }
    
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
    
}
