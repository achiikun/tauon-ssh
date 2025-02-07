package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.terminal.model.TerminalApplicationTitleListener;
import com.jediterm.terminal.ui.JediTermWidget;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.services.SettingsService;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.components.closabletabs.TabHandle;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.terminal.ssh.DisposableTtyConnector;
import tauon.app.ui.containers.session.pages.terminal.ssh.SshTtyConnector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TerminalComponent extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(TerminalComponent.class);
    
    private final JPanel contentPane;
    private final JediTermWidget term;
    private DisposableTtyConnector tty;
    private String name;
    private final Box reconnectionBox;
    
    private TabHandle tabHandle;

    public TerminalComponent(SessionInfo info, String name, String command, SessionContentPanel sessionContentPanel) {
        setLayout(new BorderLayout());
        System.out.println("Current terminal font: " + SettingsService.getSettings().getTerminalFontName());
        this.name = name;
        contentPane = new JPanel(new BorderLayout());
        JRootPane rootPane = new JRootPane();
        rootPane.setContentPane(contentPane);
        add(rootPane);
        
        term = new CustomJediterm(new CustomizedSettingsProvider(sessionContentPanel));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("Requesting focus");
                term.requestFocusInWindow();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                System.out.println("Hiding focus");
            }
        });

        tty = new SshTtyConnector(info, command, sessionContentPanel);

        reconnectionBox = Box.createHorizontalBox();
        reconnectionBox.setOpaque(true);
        reconnectionBox.setBackground(Color.RED);
        // TODO i18n
        reconnectionBox.add(new JLabel("Session not connected"));
        JButton btnReconnect = new JButton("Reconnect");
        btnReconnect.addActionListener(e -> {
            contentPane.remove(reconnectionBox);
            contentPane.revalidate();
            contentPane.repaint();
            tty = new SshTtyConnector(info, command, sessionContentPanel);
            term.setTtyConnector(tty);
            // Quick fix: terminal sets cursor invisible if disconnected, set it back to visible after reconnecting
            term.getTerminal().setCursorVisible(true);
            term.start();
        });
        reconnectionBox.add(Box.createHorizontalGlue());
        reconnectionBox.add(btnReconnect);
        reconnectionBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        term.addListener((e) -> {
            System.out.println("Disconnected");
            SwingUtilities.invokeLater(() -> {
                contentPane.add(reconnectionBox, BorderLayout.NORTH);
                contentPane.revalidate();
                contentPane.repaint();
            });
        });
        term.setTtyConnector(tty);
        term.getTerminal().addApplicationTitleListener(new TerminalApplicationTitleListener() {
            @Override
            public void onApplicationTitleChanged(@Nls @NotNull String newApplicationTitle) {
                LOG.debug("Terminal requested a new title: {}", newApplicationTitle);
                TerminalComponent.this.name = newApplicationTitle;
                SwingUtilities.invokeLater(() -> tabHandle.setTitle(newApplicationTitle));
            }
        });
        contentPane.add(term);

    }
    
    public void setTabHandle(TabHandle tabHandle) {
        this.tabHandle = tabHandle;
        this.tabHandle.setTitle(toString());
        this.tabHandle.setClosable(this::close);
    }
    
    @Override
    public String toString() {
        return "Terminal " + this.name;
    }

    public boolean close() {
        System.out.println("Closing terminal..." + name);
        this.term.close();
        return true;
    }

    public void sendCommand(String command) {
        // TODO use the same method as sending the password. Plus, is userInput, i want to type ahead.
        this.term.getTerminalStarter().sendString(command, false); // Disable type ahead
    }

    /**
     * @return the term
     */
    public JediTermWidget getTerm() {
        return term;
    }

    public void start() {
        term.start();
    }

}
