/**
 *
 */
package tauon.app.ui.containers.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.settings.SiteInfo;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.containers.session.AbstractSessionContentPanel;
import tauon.app.ui.containers.session.LocalSessionContentPanel;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.Constants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListDataEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SessionListPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(SessionListPanel.class);
    
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
    private final DefaultListModel<AbstractSessionContentPanel> sessionListModel;
    private final JList<AbstractSessionContentPanel> sessionList;
    private final AppWindow window;
    private boolean collapsed;
    
    private CollapsedPopup collapsedPopup;
    
    /**
     *
     */
    public SessionListPanel(AppWindow window) {
        super(new BorderLayout());
        this.window = window;
        sessionListModel = new DefaultListModel<>();
        sessionList = new JList<>(sessionListModel);
        sessionList.setCursor(DEFAULT_CURSOR);

        SessionListRenderer r = new SessionListRenderer();
        sessionList.setCellRenderer(r);
        
        collapsedPopup = new CollapsedPopup(sessionList);

        JScrollPane scrollPane = new SkinnedScrollPane(sessionList);
        this.add(scrollPane);
        sessionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedIndex = sessionList.getSelectedIndex();
                int index = sessionList.locationToIndex(e.getPoint());
                if (index != -1 && selectedIndex == index) {
                    Rectangle r = sessionList.getCellBounds(index, index);
                    if (r != null && r.contains(e.getPoint())) {
                        int x = e.getPoint().x;
                        int y = e.getPoint().y;

                        if (isMouseOnEjectButton(r, x, y)) {
                            removeSession(index);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sessionList.setCursor(DEFAULT_CURSOR);
                collapsedPopup.setVisible(false);
            }
        });

        sessionList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = sessionList.locationToIndex(e.getPoint());
                if (index != -1) {
                    Rectangle r = sessionList.getCellBounds(index, index);
                    if (r != null && r.contains(e.getPoint())) {
                        int x = e.getPoint().x;
                        int y = e.getPoint().y;

                        Rectangle absolute = new Rectangle(r);
                        
                        if(collapsed){
                            collapsedPopup.showOn(sessionListModel.get(index), absolute);
                        }
                        
                        if (isMouseOnEjectButton(r, x, y)) {
                            sessionList.setCursor(HAND_CURSOR);
                            return;
                        }
                    }else{
                        collapsedPopup.setVisible(false);
                    }
                }
                sessionList.setCursor(DEFAULT_CURSOR);
            }
            
        });

        sessionList.addListSelectionListener(e -> {
            System.out.println("called for index: " + sessionList.getSelectedIndex() + " " + e.getFirstIndex() + " "
                    + e.getLastIndex() + e.getValueIsAdjusting());
            if (!e.getValueIsAdjusting()) {
                int index = sessionList.getSelectedIndex();
                if (index != -1) {
                    this.selectSession(index);
                }
            }
        });
    }
    
    private boolean isMouseOnEjectButton(Rectangle cellBounds, int mouseX, int mouseY) {
        if(collapsed){
            return mouseX > cellBounds.x + cellBounds.width - 25
                    && mouseX < cellBounds.x + cellBounds.width
                    && mouseY > cellBounds.y + cellBounds.height - 25
                    && mouseY < cellBounds.y + cellBounds.height - 5;
        }else{
            return mouseX > cellBounds.x + cellBounds.width - 30
                    && mouseX < cellBounds.x + cellBounds.width
                    && mouseY > cellBounds.y + 10
                    && mouseY < cellBounds.y + cellBounds.height - 10;
        }
    }
    
    public void createSession(SiteInfo info) {
        SessionContentPanel panel = new SessionContentPanel(info, window);
        sessionListModel.insertElementAt(panel, sessionListModel.size());
        sessionList.setSelectedIndex(sessionListModel.size()-1);
        
    }
    
    public void createLocalSession() {
        AbstractSessionContentPanel m = sessionListModel.get(0);
        if(m instanceof LocalSessionContentPanel){
            sessionList.setSelectedIndex(0);
        }else{
            LocalSessionContentPanel currentLocalSessionPanel = new LocalSessionContentPanel(window);
            sessionListModel.insertElementAt(currentLocalSessionPanel, 0);
            sessionList.setSelectedIndex(0);
        }
    }

    public void selectSession(int index) {
        AbstractSessionContentPanel sessionContentPanel = sessionListModel.get(index);
        window.showSession(sessionContentPanel);
        window.revalidate();
        window.repaint();
    }

    public void removeSession(int index) {
        if (JOptionPane.showConfirmDialog(window, getBundle().getString("app.session.action.disconnect_session")) == JOptionPane.YES_OPTION) {
            AbstractSessionContentPanel sessionContentPanel = sessionListModel.get(index);
            sessionContentPanel.closeAsync((success) -> {
                if(success) {
                    window.removeSession(sessionContentPanel);
                    window.revalidate();
                    window.repaint();
                    sessionListModel.remove(index);
                    if (sessionListModel.isEmpty()) {
                        return;
                    }
                    if (index == sessionListModel.size()) {
                        sessionList.setSelectedIndex(index - 1);
                    } else {
                        sessionList.setSelectedIndex(index);
                    }
                }
            });
            
        }
    }
    
    public AbstractSessionContentPanel findSessionById(UUID uuid) {
        for(int i = 0; i < sessionListModel.getSize(); i++){
            if(sessionListModel.get(i).getUUID().equals(uuid))
                return sessionListModel.get(i);
        }
        return null;
    }
    
    public void collapsed(boolean collapsed) {
        this.collapsed = collapsed;
        
        // Hack to trigger the whole rendering of the list
        sessionListModel.getListDataListeners()[0].contentsChanged(new ListDataEvent(sessionListModel, ListDataEvent.CONTENTS_CHANGED, 0, sessionListModel.size()));
    }
    
    public final class CollapsedPopup extends JPopupMenu{
        
        private final JPanel panel;
        private final JLabel lblText;
        private final JLabel lblHost;
        private final Component owner;
        
        public CollapsedPopup(Component owner) {
            this.owner = owner;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0,0,0,0));
            
            lblText = new JLabel();
            lblHost = new JLabel();
            
            lblText.setFont(App.skin.getDefaultFont().deriveFont(Constants.SMALL_TEXT_SIZE));
            lblHost.setFont(App.skin.getDefaultFont().deriveFont(Constants.TINY_TEXT_SIZE));
            
            lblText.setText("Sample server");
            lblHost.setText("server host");
            
            lblText.setVerticalAlignment(SwingConstants.BOTTOM);
            
            panel = new JPanel(new BorderLayout(5, 0));
            panel.setBorder(new CompoundBorder(
                    new MatteBorder(2, 0, 2, 2, Color.LIGHT_GRAY),
                    new EmptyBorder(4,10,4,4)
            ));
            panel.setOpaque(false);
            panel.add(lblText);
            panel.add(lblHost, BorderLayout.SOUTH);
            
            add(panel);
            pack();
            
        }
        
        public void showOn(AbstractSessionContentPanel sessionContentPanel, Rectangle cellRectangle) {
            
            if(sessionContentPanel instanceof SessionContentPanel) {
                SiteInfo info = ((SessionContentPanel) sessionContentPanel).getInfo();
                
                lblText.setText(info.getName());
                lblHost.setText(info.getHost());
            }else{
                lblText.setText("Local Terminal"); // TODO i18n
                lblHost.setText("localhost"); // TODO i18n
            }
            
            Dimension d = new Dimension(150, (int) cellRectangle.getHeight());
            panel.setPreferredSize(d);
            panel.setMaximumSize(d);
            
            collapsedPopup.show(owner, (int) cellRectangle.getMaxX(), (int) cellRectangle.getY());
        }
    }
    
    public final class SessionListRenderer implements ListCellRenderer<AbstractSessionContentPanel> {

        private final JPanel panel;
        private final JLabel lblIcon;
        private final JLabel lblText;
        private final JLabel lblIconCollapsed;
        private final JLabel lblHost;
        private final JLabel lblClose;
        private final JLabel lblCloseCollapsed;
        private final JPanel textHolder;
        private final JPanel collapsedPanel;
        
        /**
         *
         */
        public SessionListRenderer() {
            lblIcon = new JLabel();
            lblIconCollapsed = new JLabel();
            lblText = new JLabel();
            lblHost = new JLabel();
            lblClose = new JLabel();
            lblCloseCollapsed = new JLabel();

            lblIcon.setFont(App.skin.getIconFont().deriveFont(24.0f));
            lblIconCollapsed.setFont(App.skin.getIconFont().deriveFont(18.0f));
            lblText.setFont(App.skin.getDefaultFont().deriveFont(Constants.SMALL_TEXT_SIZE));
            lblHost.setFont(App.skin.getDefaultFont().deriveFont(Constants.TINY_TEXT_SIZE));
            lblClose.setFont(App.skin.getIconFont().deriveFont(Constants.MEDIUM_TEXT_SIZE));
            lblCloseCollapsed.setFont(App.skin.getIconFont().deriveFont(Constants.TINY_TEXT_SIZE));

            lblText.setText("Sample server");
            lblHost.setText("server host");
            lblIcon.setText(FontAwesomeContants.FA_CUBE);
            lblIconCollapsed.setText(FontAwesomeContants.FA_CUBE);
            lblClose.setText(FontAwesomeContants.FA_EJECT);
            lblCloseCollapsed.setText(FontAwesomeContants.FA_EJECT);
            
            lblCloseCollapsed.setLocation(-10, 0);
            lblCloseCollapsed.setHorizontalAlignment(SwingConstants.RIGHT);

            textHolder = new JPanel(new BorderLayout(5, 0));
            textHolder.setOpaque(false);
            textHolder.add(lblText);
            textHolder.add(lblHost, BorderLayout.SOUTH);

            panel = new JPanel(new BorderLayout(5, 5));
            panel.add(lblIcon, BorderLayout.WEST);
            panel.add(lblClose, BorderLayout.EAST);
            panel.add(textHolder);

            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.setBackground(App.skin.getDefaultBackground());
            panel.setOpaque(true);

            Dimension d = panel.getPreferredSize();
            panel.setPreferredSize(d);
            panel.setMaximumSize(d);
            
            collapsedPanel = new JPanel(new BorderLayout(0, 0));
            collapsedPanel.add(lblIconCollapsed, BorderLayout.NORTH);
            collapsedPanel.add(lblCloseCollapsed, BorderLayout.SOUTH);
            
            collapsedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            collapsedPanel.setBackground(App.skin.getDefaultBackground());
            collapsedPanel.setOpaque(true);
            
            d = collapsedPanel.getPreferredSize();
            collapsedPanel.setPreferredSize(d);
            collapsedPanel.setMaximumSize(d);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AbstractSessionContentPanel> list,
                                                      AbstractSessionContentPanel value, int index, boolean isSelected, boolean cellHasFocus) {
            if(value instanceof SessionContentPanel) {
                SiteInfo info = ((SessionContentPanel) value).getInfo();
                
                lblText.setText(info.getName());
                lblHost.setText(info.getHost());
            }else{
                lblText.setText("Local Terminal"); // TODO i18n
                lblHost.setText("localhost"); // TODO i18n
            }
            
            JPanel myPanel;
            if (collapsed) {
                myPanel = collapsedPanel;
            } else {
                myPanel = panel;
            }
            
            if (isSelected) {
                myPanel.setBackground(App.skin.getDefaultSelectionBackground());
                lblText.setForeground(App.skin.getDefaultSelectionForeground());
                lblHost.setForeground(App.skin.getDefaultSelectionForeground());
                lblIcon.setForeground(App.skin.getDefaultSelectionForeground());
                lblIconCollapsed.setForeground(App.skin.getDefaultSelectionForeground());
            } else {
                myPanel.setBackground(App.skin.getDefaultBackground());
                lblText.setForeground(App.skin.getDefaultForeground());
                lblHost.setForeground(App.skin.getInfoTextForeground());
                lblIcon.setForeground(App.skin.getDefaultForeground());
                lblIconCollapsed.setForeground(App.skin.getDefaultForeground());
            }
            return myPanel;
        }
    }
}
