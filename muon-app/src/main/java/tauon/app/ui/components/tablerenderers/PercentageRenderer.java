package tauon.app.ui.components.tablerenderers;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PercentageRenderer extends JPanel
        implements TableCellRenderer {
    private final JProgressBar progressBar;

    public PercentageRenderer() {
        super(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        add(progressBar);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground()
                : table.getBackground());
        setForeground(isSelected ? table.getSelectionForeground()
                : table.getForeground());
        double pct = 0;
        if(value instanceof Double){
            pct = (Double) value;
        }else if(value instanceof PercentageValue){
            pct = ((PercentageValue) value).getValue();
        }
        if (pct > 100) {
            pct = 100;
        } else if (pct < 0) {
            pct = 0;
        }
        progressBar.setValue((int) pct);
        return this;
    }
}
