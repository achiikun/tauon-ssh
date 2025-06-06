package tauon.app.ui.components.closabletabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.util.misc.Constants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClosableTabbedPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ClosableTabbedPanel.class);
    
    private final Color unselectedBg = App.skin.getSelectedTabColor();
    private final Color selectedBg = App.skin.getDefaultBackground();
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JPanel tabHolder;
    private final JPanel buttonsBox;

    /**
     * Create a tabbed pane with closable tabs
     *
     * @param newTabCallback Called whenever new tab button is clicked
     */
    public ClosableTabbedPanel(final Consumer<JButton> newTabCallback) {
        super(new BorderLayout(0, 0), true);
        setOpaque(true);
        tabHolder = new JPanel(new GridLayout(1, 0, 0, 0));
        tabHolder.setOpaque(true);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        JPanel tabTop = new JPanel(new BorderLayout(3, 3));
        tabTop.setOpaque(true);
        tabTop.add(tabHolder);

        JButton btn = new JButton();
        btn.setToolTipText("New tab");
        btn.setFont(App.skin.getIconFont().deriveFont(Constants.MEDIUM_TEXT_SIZE));
        btn.setText(FontAwesomeContants.FA_PLUS_SQUARE);
        btn.putClientProperty("Nimbus.Overrides",
                App.skin.createTabButtonSkin());
        btn.setForeground(App.skin.getInfoTextForeground());
        btn.addActionListener(e -> {
            System.out.println("Callback called");
            newTabCallback.accept(btn);
        });
        buttonsBox = new JPanel(new GridLayout(1, 0));
        buttonsBox.setOpaque(true);
        buttonsBox.setBackground(App.skin.getDefaultBackground());
        buttonsBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        buttonsBox.add(btn);
        tabTop.add(buttonsBox, BorderLayout.EAST);

        add(tabTop, BorderLayout.NORTH);
        add(cardPanel);
    }

    public TabHandle addTab(Component body) {
        int index = tabHolder.getComponentCount();
        cardPanel.add(body, body.hashCode() + "");

        TabTitleComponent titleComponent = new TabTitleComponent();

        titleComponent.setName(body.hashCode() + "");
        titleComponent.component = body;

        tabHolder.add(titleComponent);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < tabHolder.getComponentCount(); i++) {
                    JComponent c = (JComponent) tabHolder.getComponent(i);
                    if (c == titleComponent) {
                        setSelectedIndex(i);
                        break;
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (titleComponent.tabCloseButton != null)
                    titleComponent.tabCloseButton.setHoveringTab(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (titleComponent.tabCloseButton != null)
                    titleComponent.tabCloseButton.setHoveringTab(false);
            }
        };

        titleComponent.addMouseListener(mouseAdapter);
        titleComponent.titleLabel.addMouseListener(mouseAdapter);

        setSelectedIndex(index);
        
        return titleComponent;
    }
    
    public void close(TabHandle tabHandle){
        for (int i = 0; i < tabHolder.getComponentCount(); i++) {
            JComponent c = (JComponent) tabHolder.getComponent(i);
            if (c == tabHandle) {
                removeTabAt(i, c.getName(), (TabTitleComponent) tabHandle);
                break;
            }
        }
    }

    public int getSelectedIndex() {
        for (int i = 0; i < tabHolder.getComponentCount(); i++) {
            if (tabHolder.getComponent(i) instanceof TabTitleComponent) {
                TabTitleComponent c = (TabTitleComponent) tabHolder
                        .getComponent(i);
                if (c.selected) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setSelectedIndex(int n) {
        JComponent c = (JComponent) tabHolder.getComponent(n);
        if (c instanceof TabTitleComponent) {
            String id = c.getName();
            cardLayout.show(cardPanel, id);
            for (int i = 0; i < tabHolder.getComponentCount(); i++) {
                JComponent cc = (JComponent) tabHolder.getComponent(i);
                if (cc instanceof TabTitleComponent) {
                    ((TabTitleComponent) cc).unSelect();
                }
            }
            JComponent cc = (JComponent) tabHolder.getComponent(n);
            if (cc instanceof TabTitleComponent) {
                ((TabTitleComponent) cc).select();
            }
        }
    }

    private void removeTabAt(int index, String name, TabTitleComponent title) {
        tabHolder.remove(title);
        cardPanel.remove(title.component);
        if (index > 0) {
            setSelectedIndex(index - 1);
        } else if (cardPanel.getComponentCount() > index) {
            setSelectedIndex(index);
        }
        tabHolder.revalidate();
        tabHolder.repaint();
    }

    public Component getSelectedContent() {
        for (int i = 0; i < tabHolder.getComponentCount(); i++) {
            if (tabHolder.getComponent(i) instanceof TabTitleComponent) {
                TabTitleComponent c = (TabTitleComponent) tabHolder
                        .getComponent(i);
                if (c.selected) {
                    return c.component;
                }
            }
        }
        return null;
    }

    /**
     * @return the buttonsBox
     */
    public JPanel getButtonsBox() {
        return buttonsBox;
    }

    public Component[] getTabContents() {
        return cardPanel.getComponents();
    }

    private class TabTitleComponent extends JPanel implements TabHandle{
        JLabel titleLabel;
        
        TabCloseButton tabCloseButton;
        boolean selected;
        Component component;
        
        private Supplier<Boolean> onCloseCallback;
        
        public TabTitleComponent() {
            super(new BorderLayout());
            setBorder(
                    new CompoundBorder(new MatteBorder(0, 0, 0, 1, selectedBg),
                            new EmptyBorder(5, 10, 5, 5)
                    )
            );
            setBackground(unselectedBg);
            setOpaque(true);
            titleLabel = new JLabel();
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            this.add(titleLabel);

//            if (closable) {
//                tabCloseButton = new TabCloseButton();
//                tabCloseButton.setBackground(Color.RED);
//                tabCloseButton.setForeground(App.skin.getInfoTextForeground());
//                this.add(tabCloseButton, BorderLayout.EAST);
//            }
        }
        
        @Override
        public void setTitle(String title) {
            titleLabel.setText(title);
        }
        
        @Override
        public void setClosable(Supplier<Boolean> onCloseCallback) {
            
            if(onCloseCallback != null) {
                
                if (tabCloseButton == null) {
                    tabCloseButton = new TabCloseButton();
                    tabCloseButton.setSelected(selected);
                    tabCloseButton.setForeground(App.skin.getInfoTextForeground());
                    tabCloseButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            LOG.trace("Asking for close tab: {}", getName());
                            if (TabTitleComponent.this.onCloseCallback.get()) {
                                LOG.trace("Closing tab: {}", getName());
                                ClosableTabbedPanel.this.close(TabTitleComponent.this);
                            }else{
                                LOG.trace("Cancelled closing tab: {}", getName());
                            }
                        }
                    });
                }
                
                if(tabCloseButton.getParent() == null){
                    this.add(tabCloseButton, BorderLayout.EAST);
                }
                
            }else{
                if(tabCloseButton != null){
                    this.remove(tabCloseButton);
                }
            }
            
            this.onCloseCallback = onCloseCallback;
            
        }
        
        public void unSelect() {
            selected = false;
            
            setBackground(unselectedBg);
            if (tabCloseButton != null)
                tabCloseButton.setSelected(false);
            revalidate();
            repaint();
        }
        
        public void select() {
            selected = true;
            
            setBackground(selectedBg);
            if (tabCloseButton != null)
                tabCloseButton.setSelected(true);
            revalidate();
            repaint();
        }
    }
}
