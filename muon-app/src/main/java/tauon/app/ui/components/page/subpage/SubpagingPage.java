package tauon.app.ui.components.page.subpage;

import tauon.app.App;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.page.Page;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SubpagingPage extends Page {
    
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final SessionContentPanel holder;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    
    private final List<SubpageHandler> handlerList = new ArrayList<>();
    
    /**
     *
     */
    public SubpagingPage(SessionContentPanel holder) {
        super(new BorderLayout());
        this.holder = holder;
    }
    
    @Override
    public void onLoad() {
        if (!init.get()) {
            init.set(true);
            onCreateSubpages(holder);
            createUI();
        }
    }
    
    /**
     *
     */
    private void createUI() {
        
        ButtonGroup bg = new ButtonGroup();
        Box vbox = Box.createVerticalBox();
        
        List<Component> buttons = new ArrayList<>(handlerList.size());
        for(SubpageHandler h: handlerList){
            buttons.add(h.button);
        }
        
        LayoutUtilities.equalizeSize(buttons.toArray(Component[]::new));
        
        vbox.setBorder(
                new MatteBorder(0, 0, 0, 1, App.skin.getDefaultBorderColor()));
        
        for(SubpageHandler h: handlerList){
            h.button.setAlignmentX(Box.LEFT_ALIGNMENT);
            vbox.add(h.button);
        }
        
        vbox.add(Box.createVerticalGlue());
        
        for(SubpageHandler h: handlerList){
            bg.add(h.button);
        }
        
        JScrollPane jsp = new SkinnedScrollPane(vbox);
        jsp.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.add(jsp, BorderLayout.WEST);
        
        handlerList.get(0).button.setSelected(true);
        
        revalidate();
        repaint();
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        
        for(SubpageHandler h: handlerList){
            h.button.addActionListener(e -> cardLayout.show(cardPanel, h.key));
            cardPanel.add(h.subpage, h.key);
        }
        
        this.add(cardPanel);
    }
    
    public void addSubpage(String key, String name, String icon, Subpage subpage){
    
        handlerList.add(new SubpageHandler(key, name, icon, subpage));
        
    }
    
    public abstract void onCreateSubpages(SessionContentPanel holder);
    
    private static class SubpageHandler{
        
        public SubpagingPage.Button button;
        public String key;
        public Subpage subpage;
        
        public SubpageHandler(String key, String name, String icon, Subpage subpage) {
            this.key = key;
            this.subpage = subpage;
            button = new SubpagingPage.Button(name, icon);
        }
    }
    
    /**
     * @author subhro
     *
     */
    public static class Button extends JToggleButton {
        private final String text;
        private final String iconText;
        private final Font iconFont;
    
        /**
         *
         */
        public Button(String text, String iconText) {
            this.text = text;
            this.iconText = iconText;
            this.iconFont = App.skin.getIconFont().deriveFont(24.0f);
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(
                    super.isSelected() ? App.skin.getDefaultSelectionBackground()
                            : getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(
                    super.isSelected() ? App.skin.getDefaultSelectionForeground()
                            : getForeground());
            FontMetrics fm1 = g2.getFontMetrics(iconFont);
            FontMetrics fm2 = g2.getFontMetrics(getFont());
            int y = getHeight() / 2 - (fm1.getHeight() + fm2.getHeight()) / 2;
            g2.setFont(iconFont);
            g2.drawString(iconText, getWidth() / 2 - fm1.stringWidth(iconText) / 2,
                    y + fm1.getAscent());
            g2.setFont(getFont());
            g2.drawString(text, getWidth() / 2 - fm2.stringWidth(text) / 2,
                    y + fm1.getHeight() + 5 + fm2.getAscent());
        }
    
        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm1 = getFontMetrics(getFont());
            FontMetrics fm2 = getFontMetrics(iconFont);
            int w1 = fm1.stringWidth(text);
            int w2 = fm2.stringWidth(iconText);
            int h1 = fm1.getHeight();
            int h2 = fm2.getHeight();
            return new Dimension(Math.max(w1, w2) + 10, h1 + h2 + 30);
        }
    
        @Override
        public Dimension getMinimumSize() {
            return this.getPreferredSize();
        }
    
        @Override
        public Dimension getMaximumSize() {
            Dimension d = this.getPreferredSize();
            return new Dimension(Short.MAX_VALUE, d.height);
        }
    
    }
}
