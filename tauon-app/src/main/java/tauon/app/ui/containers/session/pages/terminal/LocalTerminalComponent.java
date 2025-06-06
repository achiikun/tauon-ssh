package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsConfigManager;
import tauon.app.ssh.GuiHandle;
import tauon.app.util.misc.PlatformUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class LocalTerminalComponent extends AbstractTerminalComponent {
    private static final Logger LOG = LoggerFactory.getLogger(LocalTerminalComponent.class);
    private @NotNull PtyProcess process;
    
    public LocalTerminalComponent(String name, String initialCommand, GuiHandle guiHandle) {
        super(name, initialCommand, guiHandle);
        createUi();
    }
    
    @Override
    protected TtyConnector createTty(String initialCommand) throws IOException {
        // Get terminal settings
        String terminalType = SettingsConfigManager.getSettings().getTerminalType(); // Example: "xterm-256color"
        int termWidth = SettingsConfigManager.getSettings().getTermWidth();         // Example: 80
        int termHeight = SettingsConfigManager.getSettings().getTermHeight();
        
        // Create PTY process
        String[] command2;
        
        if (PlatformUtils.IS_WINDOWS) {
            command2 = new String[]{"cmd.exe"};
        } else if (PlatformUtils.IS_MAC) {
            command2 = new String[]{"/bin/zsh", "--login"}; // TODO Customize shell
        } else {
            command2 = new String[]{"/bin/bash", "--login"}; // TODO Customize shell
        }
        HashMap<String, String> env = new HashMap<>(System.getenv());
        
        // Set ANSI terminal support and language environment variable
        env.put("TERM", terminalType);
        env.put("LANG", "en_US.UTF-8");
        
        process = new PtyProcessBuilder()
                .setCommand(command2)
                .setDirectory(System.getProperty("user.home"))
                .setEnvironment(env)
                .setInitialColumns(termWidth)
                .setInitialRows(termHeight)
                .start();
        
        return new ProcessTtyConnector(process, StandardCharsets.UTF_8) {
            @Override
            public String getName() {
                return "Local terminal";
            }
            
            @Override
            public void resize(@NotNull TermSize termSize) {
                int columns = termSize.getColumns();
                int rows = termSize.getRows();
                
                WinSize winSize = new WinSize(columns, rows);
                process.setWinSize(winSize);
            }
        };
    }
    
    @Override
    protected void onTermReportedDisconnection(TtyConnector ttyConnector) {
    
    }
    
    @Override
    protected void onDisposeSession() {
        // The process is destroyed when the TtyConnector is closed
//        this.process.destroyForcibly();
    }
}
