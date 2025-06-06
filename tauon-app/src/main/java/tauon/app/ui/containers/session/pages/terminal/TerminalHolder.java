package tauon.app.ui.containers.session.pages.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.SSHConnectionHandler;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalHolder extends AbstractTerminalHolder implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TerminalHolder.class);
    
    private final AtomicBoolean init = new AtomicBoolean(false);
    
    private int c = 1;
    
    private final SSHConnectionHandler connectionHandler;
    
    public TerminalHolder(GuiHandle guiHandle, SSHConnectionHandler connectionHandler) {
        super(guiHandle);
        this.connectionHandler = connectionHandler;
        
        try {
            openNewTerminal(null, false); // It is started at onLoad()
        } catch (SessionClosedException e) {
            guiHandle.reportException(e);
        }
        
//
//        this.tabs = new ClosableTabbedPanel(e -> {
//            try {
//                openNewTerminal(null);
//            } catch (SessionClosedException ex) {
//                guiHandle.reportException(ex);
//            }
//        });
//
//        btn = new JButton();
//        btn.setToolTipText("Snippets");
//        btn.addActionListener(e -> {
//            showSnippets();
//        });
//        btn.setFont(App.skin.getIconFont().deriveFont(Constants.MEDIUM_TEXT_SIZE));
//        btn.setText(FontAwesomeContants.FA_BOOKMARK);
//        btn.putClientProperty("Nimbus.Overrides", App.skin.createTabButtonSkin());
//        btn.setForeground(App.skin.getInfoTextForeground());
//        tabs.getButtonsBox().add(btn);
//
//        long t1 = System.currentTimeMillis();
//        try {
//            TerminalComponent tc = new TerminalComponent(String.valueOf(c), null, guiHandle, connectionHandler.openSessionHandle());
//            tc.setTabHandle(this.tabs.addTab(tc));
//        } catch (SessionClosedException e) {
//            guiHandle.reportException(e);
//        }
//
//        long t2 = System.currentTimeMillis();
//        LOG.debug("Terminal was init in: {} ms", t2 - t1);
//
//        snippetPanel = new SnippetPanel(e -> {
//            TerminalComponent tc1 = (TerminalComponent) tabs.getSelectedContent();
//            tc1.sendCommand(e);// + "\n"); I don't want to send the new line
//        }, e -> {
//            this.snippetPopupMenu.setVisible(false);
//        });
//        snippetPopupMenu = new JPopupMenu();
//        snippetPopupMenu.add(snippetPanel);
//        this.add(tabs);
//
//        addAncestorListener(new AncestorListener() {
//
//            @Override
//            public void ancestorRemoved(AncestorEvent event) {
//
//            }
//
//            @Override
//            public void ancestorMoved(AncestorEvent event) {
//
//            }
//
//            @Override
//            public void ancestorAdded(AncestorEvent event) {
//                System.err.println("Terminal ancestor component shown");
//                focusTerminal();
//            }
//        });
//
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentShown(ComponentEvent e) {
//                focusTerminal();
//            }
//        });
    }

    @Override
    public void onLoad() {
        if (init.get()) {
            return;
        }
        init.set(true);
        TerminalComponent tc = (TerminalComponent) this.tabs.getSelectedContent();
        tc.start();
    }

    public void close() {
        Component[] components = tabs.getTabContents();
        for (Component c : components) {
            if (c instanceof TerminalComponent) {
                System.out.println("Closing terminal: " + c);
                ((TerminalComponent) c).close();
            }
        }
        super.close();
    }
    
    @Override
    public void onNewTabClicked() throws SessionClosedException {
        openNewTerminal(null, true);
    }
    
    public void openNewTerminal(String command, boolean start) throws SessionClosedException {
        c++;
        TerminalComponent tc = new TerminalComponent(
                String.valueOf(c),
                command,
                guiHandle,
                connectionHandler.openSessionHandle()
        );
        tc.setTabHandle(this.tabs.addTab(tc));
        if(start)
            tc.start();
    }
}
