/**
 *
 */
package tauon.app.ui.components.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SkinnedTextField extends JTextField {
    private static final Logger LOG = LoggerFactory.getLogger(SkinnedTextField.class);
    
    /**
     *
     */
    public SkinnedTextField() {
        super();
        installPopUp();
    }

    public SkinnedTextField(int cols) {
        super(cols);
        installPopUp();
    }

    private void installPopUp() {
        this.putClientProperty("flat.popup", createPopup());
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Right click on text field");
                if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {

                    JPopupMenu pop = (JPopupMenu) SkinnedTextField.this
                            .getClientProperty("flat.popup");
                    if (pop != null) {
                        pop.show(SkinnedTextField.this, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private JPopupMenu createPopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mCut = new JMenuItem(getBundle().getString("utils.textarea.action.cut"));
        JMenuItem mCopy = new JMenuItem(getBundle().getString("utils.textarea.action.copy"));
        JMenuItem mPaste = new JMenuItem(getBundle().getString("utils.textarea.action.paste"));
        JMenuItem mSelect = new JMenuItem(getBundle().getString("utils.textarea.action.select_all"));

        popup.add(mCut);
        popup.add(mCopy);
        popup.add(mPaste);
        popup.add(mSelect);

        mCut.addActionListener(e -> {
            cut();
        });

        mCopy.addActionListener(e -> {
            copy();
        });

        mPaste.addActionListener(e -> {
            paste();
        });

        mSelect.addActionListener(e -> {
            selectAll();
        });

        return popup;
    }
}
