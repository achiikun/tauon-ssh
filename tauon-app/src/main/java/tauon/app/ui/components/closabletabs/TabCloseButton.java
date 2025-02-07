/**
 *
 */
package tauon.app.ui.components.closabletabs;

import tauon.app.App;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * @author subhro
 *
 */
public class TabCloseButton extends JComponent {
    /**
     *
     */
    private boolean hoveringTab;
    private boolean hoveringButton;
    private boolean selected;
    private final Font font;

    public TabCloseButton() {
        setPreferredSize(new Dimension(20, 20));
        setMinimumSize(new Dimension(20, 20));
        setMaximumSize(new Dimension(20, 20));
        font = App.skin.getIconFont().deriveFont(14.0f);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoveringButton = true;
                repaint(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveringButton = false;
                repaint(0);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if(hoveringButton){
            if(selected) {
                g2.setColor(App.skin.getSelectedTabColor());
            }else{
                g2.setColor(App.skin.getDefaultBackground());
            }
        }else{
            g2.setColor(getBackground());
        }
        
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        boolean drawButton = selected || hoveringTab || hoveringButton;
        if (drawButton) {
            g2.setColor(getForeground());
            g2.setFont(font);
            
            Rectangle2D metrics = g2.getFontMetrics().getStringBounds(FontAwesomeContants.FA_WINDOW_CLOSE, g2);
            g2.drawString(
                    FontAwesomeContants.FA_WINDOW_CLOSE,
                    ((float) (getWidth() - metrics.getWidth())) / 2,
                    (float) (metrics.getHeight() + (metrics.getHeight() + metrics.getY()) / 2)
            );
        }
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        this.repaint(0);
    }

    /**
     * @param hoveringTab the hovering to set
     */
    public void setHoveringTab(boolean hoveringTab) {
        this.hoveringTab = hoveringTab;
        this.repaint(0);
    }

}
