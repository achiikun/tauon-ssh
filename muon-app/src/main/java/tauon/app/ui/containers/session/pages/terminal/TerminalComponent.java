package tauon.app.ui.containers.session.pages.terminal;

import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalPanelListener;
import com.jediterm.terminal.ui.TerminalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.services.SettingsService;
import tauon.app.ui.components.closabletabs.ClosableTabContent;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel.TabTitle;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.containers.session.pages.info.sysload.SysLoadPage;
import tauon.app.ui.containers.session.pages.terminal.ssh.DisposableTtyConnector;
import tauon.app.ui.containers.session.pages.terminal.ssh.SshTtyConnector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TerminalComponent extends JPanel implements ClosableTabContent {
    private static final Logger LOG = LoggerFactory.getLogger(SysLoadPage.class);
    
    private final JPanel contentPane;
    private final JediTermWidget term;
    private DisposableTtyConnector tty;
    private String name;
    private final Box reconnectionBox;
    private final TabTitle tabTitle;

    public TerminalComponent(SessionInfo info, String name, String command, SessionContentPanel sessionContentPanel) {
        setLayout(new BorderLayout());
        System.out.println("Current terminal font: " + SettingsService.getSettings().getTerminalFontName());
        this.name = name;
        this.tabTitle = new TabTitle();
        contentPane = new JPanel(new BorderLayout());
        JRootPane rootPane = new JRootPane();
        rootPane.setContentPane(contentPane);
        add(rootPane);
        
        term = new CustomJediterm(new CustomizedSettingsProvider(info));

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
        term.setTerminalPanelListener(new TerminalPanelListener() {

            @Override
            public void onTitleChanged(String title) {
                System.out.println("new title: " + title);
                TerminalComponent.this.name = title;
                SwingUtilities.invokeLater(() -> tabTitle.getCallback().accept(title));
            }

            
            public void onSessionChanged(TerminalSession currentSession) {
                System.out.println("currentSession: " + currentSession);
            }

            public void onPanelResize(RequestOrigin origin) {  }
        });
        contentPane.add(term);

    }

    @Override
    public String toString() {
        return "Terminal " + this.name;
    }

    @Override
    public boolean close() {
        System.out.println("Closing terminal..." + name);
        this.term.close();
        return true;
    }

    public void sendCommand(String command) {
        this.term.getTerminalStarter().sendString(command);
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

    /**
     * @return the tabTitle
     */
    public TabTitle getTabTitle() {
        return tabTitle;
    }
}
