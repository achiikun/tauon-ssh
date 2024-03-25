/**
 *
 */
package tauon.app.ui.components.misc;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SkinnedTextArea extends JTextArea {
    /**
     *
     */
    public SkinnedTextArea() {
        installPopUp();
    }

    private void installPopUp() {
        this.putClientProperty("flat.popup", createPopup());
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Right click on text field");
                if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {

                    JPopupMenu pop = (JPopupMenu) SkinnedTextArea.this
                            .getClientProperty("flat.popup");
                    if (pop != null) {
                        pop.show(SkinnedTextArea.this, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private JPopupMenu createPopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mCut = new JMenuItem(getBundle().getString("cut"));
        JMenuItem mCopy = new JMenuItem(getBundle().getString("copy"));
        JMenuItem mPaste = new JMenuItem(getBundle().getString("paste"));
        JMenuItem mSelect = new JMenuItem(getBundle().getString("select_all"));

        popup.add(mCut);
        popup.add(mCopy);
        popup.add(mPaste);
        popup.add(mSelect);

        mCut.addActionListener(e -> cut());

        mCopy.addActionListener(e -> copy());

        mPaste.addActionListener(e -> paste());

        mSelect.addActionListener(e -> selectAll());

        return popup;
    }
}
