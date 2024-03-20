package tauon.app.ui.components.glasspanes;

import tauon.app.App;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionInputBlocker extends JPanel {
    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);
    double angle = 0.0;
    JButton btn = new JButton();
    AtomicBoolean stopFlag;
    Color c1 = new Color(3, 155, 229);
    Stroke basicStroke = new BasicStroke(15);
    Timer timer;
    float alpha = 0.65f;
    AlphaComposite alphaComposite = AlphaComposite.SrcOver.derive(alpha);
    AlphaComposite alphaComposite1 = AlphaComposite.SrcOver.derive(0.85f);
    private long time;
    
    private boolean stateHoverBtn;
    private boolean statePressedBtn;
    
    public SessionInputBlocker() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFont(App.skin.getIconFont().deriveFont(20.0f));
        btn.setText("\uf00d");
        btn.setAlignmentX(Box.CENTER_ALIGNMENT);
        setOpaque(false);
        btn.addActionListener(e -> {
            System.out.println("Stop button clicked: " + stopFlag);
            if (stopFlag != null) {
                stopFlag.set(true);
            }
        });
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                statePressedBtn = true;
                setBtnForeground();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                statePressedBtn = false;
                setBtnForeground();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                stateHoverBtn = true;
                setBtnForeground();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                stateHoverBtn = false;
                setBtnForeground();
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                stateHoverBtn = true;
                setBtnForeground();
            }
        });
        add(Box.createVerticalGlue());
        add(btn);
        add(Box.createVerticalGlue());
        int size = 400;
        timer = new Timer(20, e -> {
            this.time += 20;
            angle += Math.toRadians(5); // 5 degrees per 100 ms = 50
            // degrees/second
            while (angle > 2 * Math.PI) {
                angle -= 2 * Math.PI; // keep angle in reasonable range.
            }
            
            setBtnForeground();
            
            if(time >= 3000){
                int x = getWidth() / 2 - size / 2;
                int y = getHeight() / 2 - size / 2;
                repaint(x, y, size, size);
            }else{
                repaint();
            }

        });
        addMouseListener(new MouseAdapter() {
        });
        addMouseMotionListener(new MouseAdapter() {
        });
        addKeyListener(new KeyAdapter() {
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });
        setFocusTraversalKeysEnabled(false);
    }
    
    private void setBtnForeground() {
        
        if(stopFlag == null)
            return;
        
        if((stopFlag.get() || time <= 500) && btn.getForeground().getAlpha() == 255) {
            btn.setForeground(TRANSPARENT);
            return;
        }
        
        if(time > 500) {
            if(statePressedBtn){
                if(btn.getForeground().getAlpha() != 255)
                    btn.setForeground(TRANSPARENT);
            } else if(stateHoverBtn){
                if(!btn.getForeground().equals(Color.RED))
                    btn.setForeground(Color.RED);
            } else if(!btn.getForeground().equals(Color.WHITE)){
                btn.setForeground(Color.WHITE);
            }
        }
    }
    
    public void startAnimation(AtomicBoolean stopFlag) {
        this.time = 0;
        this.statePressedBtn = false;
        this.stateHoverBtn = false;
        btn.setForeground(TRANSPARENT);
        this.stopFlag = stopFlag;
        this.btn.setVisible(stopFlag != null);
        timer.start();
    }

    public void stopAnimation() {
        timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if(time < 500)
            return;
        
        AlphaComposite alphaComposite = null;
        
        if(time >= 3000){
            alphaComposite = this.alphaComposite;
        }else{
            float customAlpha = alpha * (1 - ((3000-time) / 2500f));
            alphaComposite = AlphaComposite.SrcOver.derive(customAlpha);
        }
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(alphaComposite);
        
        g2.setColor(Color.BLACK);
        
        Rectangle r = g.getClipBounds();
        g2.fillRect(r.x, r.y, r.width, r.height);
        
        g2.setComposite(alphaComposite1);
        g2.setStroke(basicStroke);
        int x = getWidth() / 2 - 70 / 2;
        int y = getHeight() / 2 - 70 / 2;
        if (btn.isVisible()) {
            g2.setColor(stopFlag.get() ? Color.RED : Color.BLACK);
            g2.fillOval(x + 5, y + 5, 70 - 10, 70 - 10);
        }
        g2.setColor(c1);
        g2.rotate(angle, getWidth() / 2, getHeight() / 2);
        g2.drawArc(x + 5, y + 5, 70 - 10, 70 - 10, 0, 90);
        g2.rotate(-angle, getWidth() / 2, getHeight() / 2);
    }
}
