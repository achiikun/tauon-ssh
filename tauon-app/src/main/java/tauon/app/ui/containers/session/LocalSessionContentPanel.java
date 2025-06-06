/**
 *
 */
package tauon.app.ui.containers.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.settings.PortForwardingRule;
import tauon.app.ssh.GuiHandle;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.components.page.PageHolder;
import tauon.app.ui.containers.main.AppWindow;
import tauon.app.ui.containers.session.pages.terminal.LocalTerminalHolder;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

public class LocalSessionContentPanel extends AbstractSessionContentPanel implements PageHolder, GuiHandle {
    private static final Logger LOG = LoggerFactory.getLogger(LocalSessionContentPanel.class);
    
    private final LocalTerminalHolder terminalHolder;
    
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    /**
     *
     */
    public LocalSessionContentPanel(AppWindow appWindow) {
        super(appWindow);
        
        terminalHolder = new LocalTerminalHolder(this);
        
        createUi();
        
    }
    
    @Override
    protected Page[] createPages() {
        return new Page[]{terminalHolder};
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
                terminalHolder.close();
            } catch (Exception e) {
                LOG.error("Error while closing the main session instance.", e);
                onClosed.accept(false);
            } finally {
                enableUi();
                onClosed.accept(true);
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
        return promptPassword(null, "SUDO", null, true);
    }
    
    @Override
    public void reportPortForwardingFailed(PortForwardingRule portForwardingState, IOException e) {
    
    }
    
    @Override
    public BlockHandle blockUi(Object client, UserCancelHandle userCancelHandle) {
//        if (client == this.sshConnectionHandler) {
//            BlockHandle block = () -> appWindow.getInputBlocker().unblockInput();
//            appWindow.getInputBlocker().blockInput(
//                    getBundle().getString("app.ui.status.connecting"),
//                    () -> userCancelHandle.userCancelled(block)
//            );
//            return block;
//        }
//        return () -> {
//        }; // No block
        throw new UnsupportedOperationException("Not implemented");
    }
    
}
