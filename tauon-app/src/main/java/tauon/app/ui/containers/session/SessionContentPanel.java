/**
 *
 */
package tauon.app.ui.containers.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.*;
import tauon.app.services.SettingsConfigManager;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.SiteInfo;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.ISSHOperator;
import tauon.app.ssh.IStopper;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.components.page.PageHolder;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.info.InfoPage;
import tauon.app.ui.containers.session.pages.logviewer.LogViewer;
import tauon.app.ui.containers.session.pages.terminal.TerminalHolder;
import tauon.app.ui.containers.session.pages.tools.ToolsPage;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 */
public class SessionContentPanel extends AbstractSessionContentPanel implements PageHolder, GuiHandle {
    private static final Logger LOG = LoggerFactory.getLogger(SessionContentPanel.class);
    
    private final SiteInfo info;
    
    private final FileBrowser fileBrowser;
    private final LogViewer logViewer;
    private final TerminalHolder terminalHolder;
    private final InfoPage processViewer;
    private final ToolsPage toolsPage;
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private final SSHConnectionHandler sshConnectionHandler;
    
    /**
     *
     */
    public SessionContentPanel(SiteInfo info, AppWindow appWindow) {
        super(appWindow);
        this.info = info;
        this.sshConnectionHandler = new SSHConnectionHandler(
                info,
                this,
                new MyPasswordFinder(),
                appWindow.hostKeyVerifier
        );
        
        terminalHolder = new TerminalHolder(this, sshConnectionHandler);
        fileBrowser = new FileBrowser(info, this);
        logViewer = new LogViewer(this);
        processViewer = new InfoPage(this);
        toolsPage = new ToolsPage(this);
        
        createUi();
        
    }
    
    @Override
    protected Page[] createPages() {
        if (SettingsConfigManager.getSettings().isFirstFileBrowserView()) {
            return new Page[]{fileBrowser, terminalHolder, logViewer,
                    processViewer, toolsPage};
        } else {
            return new Page[]{terminalHolder, fileBrowser, logViewer,
                    processViewer, toolsPage};
        }
    }
    
    public SSHConnectionHandler getSshConnectionHandler() {
        return sshConnectionHandler;
    }
    
    /**
     * @return the info
     */
    public SiteInfo getInfo() {
        return info;
    }
    
    public FileBrowser getFileBrowser() {
        return fileBrowser;
    }
    
//    public FileTransferProgress startFileTransferModal(Consumer<Boolean> stopCallback) {
//        rootPane.setGlassPane(this.progressPanel);
//        FileTransferProgress h = progressPanel.show(stopCallback);
//        this.revalidate();
//        this.repaint();
//        return h;
//    }
    
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
        try {
            this.terminalHolder.openNewTerminal(command, true);
        } catch (SessionClosedException e) {
            this.reportException(e);
        }
    }
    
    /**
     * @return the closed
     */
    public boolean isSessionClosed() {
        return closed.get();
    }
    
    @Override
    public void closeAsync(Consumer<Boolean> onClosed) {
        if (closed.getAndSet(true)) {
            onClosed.accept(false);
            return;
        }
        
        try {
            disableUi();
        } catch (InterruptedException | InvocationTargetException e) {
            LOG.error("Error while disabling the Ui.", e);
        }
        
        executor.submit(() -> {
            
            try {
                this.sshConnectionHandler.close();
            } catch (Exception e) {
                LOG.error("Error while closing the main session instance.", e);
                onClosed.accept(false);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    try {
                        try {
                            this.terminalHolder.close();
                        } catch (Exception e) {
                            LOG.error("Error while closing Terminal", e);
                        }
                        
                        this.fileBrowser.close();
                        
                        this.executor.shutdownNow();
                        
                        try {
                            if (!this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                                LOG.error("The background executor was not fully shutdown");
                            }
                        } catch (InterruptedException e1) {
                            LOG.error("Error while closing the background executor pool", e1);
                        }
                        
                        appWindow.getInputBlocker().unblockInput();
                        
                    } finally {
                        enableUi();
                        onClosed.accept(true);
                    }
                    
                });
            }
            
        });
        
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
    public char[] promptSudoPassword(boolean isRetrying) throws OperationCancelledException {
        if (!isRetrying) {
            // TODO set SUDO in a separate field
            return this.info.getPassword().toCharArray();
        } else {
            return promptPassword(null, "SUDO", null, true);
        }
    }
    
    @Override
    public void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e) {
    
    }
    
    @Override
    public BlockHandle blockUi(Object client, UserCancelHandle userCancelHandle) {
        if (client == this.sshConnectionHandler) {
            BlockHandle block = () -> appWindow.getInputBlocker().unblockInput();
            appWindow.getInputBlocker().blockInput(
                    getBundle().getString("app.ui.status.connecting"),
                    () -> userCancelHandle.userCancelled(block)
            );
            return block;
        }
        return () -> {
        }; // No block
    }
    
    public void runSSHOperation(ISSHOperator consumer) {
        
        boolean force = false;
        while (true) {
            try {
                sshConnectionHandler.ensureConnected(force);
                
                try {
                    try {
                        consumer.operate(this, sshConnectionHandler);
                    } catch (IOException e) {
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
    
    public void submitSSHOperation(ISSHOperator consumer) {
        submitSSHOperationStoppable(consumer, null);
    }
    
    public void submitSSHOperationStoppable(ISSHOperator consumer, IStopper.Handle stopFlag) {
        // TODO create the stopFlag here and pass it through the consumer
        executor.submit(() -> {
            
            boolean force = false;
            while (true) {
                try {
                    // Ensures connection before doing nothing, but when the operation request a new session, it will again ensure connection
                    sshConnectionHandler.ensureConnected(force);
                    
                    force = true;
                    
                    try {
                        disableUi(stopFlag);
                    } catch (InterruptedException | InvocationTargetException e) {
                        LOG.error("Error while disabling the Ui. This error should never be thrown.", e);
                    }
                    
                    try {
                        try {
                            consumer.operate(this, sshConnectionHandler);
                        } catch (IOException e) {
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
        });
    }
    
}
