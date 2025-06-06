package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsConfigManager;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ui.components.closabletabs.TabHandle;
import tauon.app.ui.containers.session.pages.terminal.ssh.DisposableTtyConnector;
import tauon.app.ui.containers.session.pages.terminal.ssh.SshTtyConnector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TerminalComponent extends AbstractTerminalComponent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminalComponent.class);
    
    private final SSHConnectionHandler.SessionHandle sessionHandle;
    
    public TerminalComponent(String name, String initialCommand, GuiHandle guiHandle, SSHConnectionHandler.SessionHandle sessionHandle) {
        super(name, initialCommand, guiHandle);
        this.sessionHandle = sessionHandle;
        createUi();
    }
    
    @Override
    protected void onTermReportedDisconnection(TtyConnector ttyConnector) {
        ((SshTtyConnector)ttyConnector).stop();
    }
    
    @Override
    protected TtyConnector createTty(String initialCommand) {
        return new SshTtyConnector(initialCommand, sessionHandle);
    }
    
    @Override
    protected void onDisposeSession() {
        // Dispose the session handle and don't use it again
        this.sessionHandle.dispose();
    }
}
