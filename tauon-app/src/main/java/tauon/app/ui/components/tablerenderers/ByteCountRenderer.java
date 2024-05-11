package tauon.app.ui.components.tablerenderers;

import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ByteCountRenderer extends JLabel implements TableCellRenderer {

    public ByteCountRenderer(){
        setText("-");
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground()
                : table.getBackground());
        setForeground(isSelected ? table.getSelectionForeground()
                : table.getForeground());
        if (value instanceof Long) {
            setText(FormatUtils.humanReadableByteCount((long) value, true));
        } else if(value instanceof ByteCountValue){
            setText(FormatUtils.humanReadableByteCount(((ByteCountValue) value).getValue(), true));
        } else {
            setText(value.toString());
        }
        return this;
    }
}
