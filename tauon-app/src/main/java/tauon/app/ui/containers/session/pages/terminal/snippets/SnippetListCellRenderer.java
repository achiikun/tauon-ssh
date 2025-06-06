package tauon.app.ui.containers.session.pages.terminal.snippets;

import tauon.app.App;
import tauon.app.util.misc.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SnippetListCellRenderer extends JPanel implements ListCellRenderer<SnippetItem> {
    private final JLabel lblName;
    private final JLabel lblCommand;
    
    public SnippetListCellRenderer() {
        super(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(5, 10, 5, 10));
        lblName = new JLabel();
        lblName.setFont(lblName.getFont().deriveFont(Font.PLAIN, Constants.SMALL_TEXT_SIZE));
        lblCommand = new JLabel();
        add(lblName);
        add(lblCommand, BorderLayout.SOUTH);
    }
    
    @Override
    public Component getListCellRendererComponent(
            JList<? extends SnippetItem> list, SnippetItem value, int index,
            boolean isSelected, boolean cellHasFocus
    ) {
        setBackground(
                isSelected
                        ? new Color(3, 155, 229)
                        : list.getBackground()
        );
        lblName.setForeground(
                isSelected
                        ? App.skin.getDefaultSelectionForeground()
                        : App.skin.getDefaultForeground()
        );
        lblCommand.setForeground(App.skin.getInfoTextForeground());
        lblName.setText(value.getName());
        lblCommand.setText(value.getCommand());
        return this;
    }
}
