package tauon.app.ui.containers.session.pages.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.GuiHandle;
import tauon.app.ui.components.closabletabs.ClosableTabbedPanel;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.containers.session.pages.terminal.snippets.SnippetPanel;
import tauon.app.util.misc.Constants;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTerminalHolder extends Page implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTerminalHolder.class);
    
    protected final ClosableTabbedPanel tabs;
    private JPopupMenu snippetPopupMenu;
    private final SnippetPanel snippetPanel;
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final JButton btn;
    
    protected final GuiHandle guiHandle;
    
    public AbstractTerminalHolder(GuiHandle guiHandle) {
        this.guiHandle = guiHandle;
        this.tabs = new ClosableTabbedPanel(e -> {
            try {
                onNewTabClicked();
            } catch (SessionClosedException ex) {
                guiHandle.reportException(ex);
            }
        });
        
        btn = new JButton();
        btn.setToolTipText("Snippets");
        btn.addActionListener(e -> {
            showSnippets();
        });
        btn.setFont(App.skin.getIconFont().deriveFont(Constants.MEDIUM_TEXT_SIZE));
        btn.setText(FontAwesomeContants.FA_BOOKMARK);
        btn.putClientProperty("Nimbus.Overrides", App.skin.createTabButtonSkin());
        btn.setForeground(App.skin.getInfoTextForeground());
        tabs.getButtonsBox().add(btn);
        
        snippetPanel = new SnippetPanel(e -> {
            AbstractTerminalComponent tc1 = (AbstractTerminalComponent) tabs.getSelectedContent();
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
    
    public abstract void onNewTabClicked() throws SessionClosedException;

//    protected void createUi(){
//
//        long t1 = System.currentTimeMillis();
//        try {
//            AbstractTerminalComponent tc = new AbstractTerminalComponent(
//                    String.valueOf(c), null, guiHandle, connectionHandler.openSessionHandle()
//            );
//            tc.setTabHandle(this.tabs.addTab(tc));
//        } catch (SessionClosedException e) {
//            guiHandle.reportException(e);
//        }
//
//        long t2 = System.currentTimeMillis();
//        LOG.debug("Terminal was init in: {} ms", t2 - t1);
//
//    }

    private void focusTerminal() {
        tabs.requestFocusInWindow();
        System.err.println("Terminal component shown");
        AbstractTerminalComponent comp = (AbstractTerminalComponent) tabs.getSelectedContent();
        if (comp != null) {
            comp.requestFocusInWindow();
            comp.getTerm().requestFocusInWindow();
            comp.getTerm().requestFocus();
        }
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

}
