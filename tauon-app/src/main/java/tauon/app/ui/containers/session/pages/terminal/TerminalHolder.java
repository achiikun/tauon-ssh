package tauon.app.ui.containers.session.pages.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.containers.session.pages.terminal.snippets.SnippetPanel;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerminalHolder extends Page implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TerminalHolder.class);
    
    private final ClosableTabbedPanel tabs;
    private JPopupMenu snippetPopupMenu;
    private final SnippetPanel snippetPanel;
    private final AtomicBoolean init = new AtomicBoolean(false);
    private int c = 1;
    private final JButton btn;
    private final SessionContentPanel sessionContentPanel;

    public TerminalHolder(SessionInfo info, SessionContentPanel sessionContentPanel) {
        this.sessionContentPanel = sessionContentPanel;
        this.tabs = new ClosableTabbedPanel(e -> {
            openNewTerminal(null);
        });

        btn = new JButton();
        btn.setToolTipText("Snippets");
        btn.addActionListener(e -> {
            showSnippets();
        });
        btn.setFont(App.skin.getIconFont().deriveFont(16.0f));
        btn.setText(FontAwesomeContants.FA_BOOKMARK);
        btn.putClientProperty("Nimbus.Overrides", App.skin.createTabButtonSkin());
        btn.setForeground(App.skin.getInfoTextForeground());
        tabs.getButtonsBox().add(btn);

        long t1 = System.currentTimeMillis();
        TerminalComponent tc = new TerminalComponent(info, String.valueOf(c), null, sessionContentPanel);
        tc.setTabHandle(this.tabs.addTab(tc));
        long t2 = System.currentTimeMillis();
        LOG.debug("Terminal was init in: {} ms", t2 - t1);

        snippetPanel = new SnippetPanel(e -> {
            TerminalComponent tc1 = (TerminalComponent) tabs.getSelectedContent();
            tc1.sendCommand(e);// + "\n"); I don't want to send the new line
        }, e -> {
            this.snippetPopupMenu.setVisible(false);
        });
        snippetPopupMenu = new JPopupMenu();
        snippetPopupMenu.add(snippetPanel);
        this.add(tabs);

        addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorRemoved(AncestorEvent event) {

            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }

            @Override
            public void ancestorAdded(AncestorEvent event) {
                System.err.println("Terminal ancestor component shown");
                focusTerminal();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                focusTerminal();
            }
        });
    }

    private void focusTerminal() {
        tabs.requestFocusInWindow();
        System.err.println("Terminal component shown");
        TerminalComponent comp = (TerminalComponent) tabs.getSelectedContent();
        if (comp != null) {
            comp.requestFocusInWindow();
            comp.getTerm().requestFocusInWindow();
            comp.getTerm().requestFocus();
        }
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

    private void showSnippets() {
        this.snippetPanel.loadSnippets();
        this.snippetPopupMenu.setLightWeightPopupEnabled(true);
        this.snippetPopupMenu.setOpaque(true);
        this.snippetPopupMenu.pack();
        this.snippetPopupMenu.setInvoker(this.btn);
        this.snippetPopupMenu.show(this.btn, this.btn.getWidth() - this.snippetPopupMenu.getPreferredSize().width,
                this.btn.getHeight());
    }

    public void close() {
        Component[] components = tabs.getTabContents();
        for (int i = 0; i < components.length; i++) {
            Component c = components[i];
            if (c instanceof TerminalComponent) {
                System.out.println("Closing terminal: " + c);
                ((TerminalComponent) c).close();
            }
        }
        revalidate();
        repaint();
    }

    @Override
    public String getIcon() {
        return FontAwesomeContants.FA_TELEVISION;
    }

    @Override
    public String getText() {
        return "Terminal";
    }

    public void openNewTerminal(String command) {
        c++;
        TerminalComponent tc = new TerminalComponent(
                this.sessionContentPanel.getInfo(),
                c + "",
                command,
                this.sessionContentPanel
        );
        tc.setTabHandle(this.tabs.addTab(tc));
        tc.start();
    }
}
