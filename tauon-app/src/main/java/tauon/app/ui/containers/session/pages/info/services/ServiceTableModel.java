/**
 *
 */
package tauon.app.ui.containers.session.pages.info.services;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class ServiceTableModel extends AbstractTableModel {
    private final String[] columns = {
            getBundle().getString("app.info_services_systemd.label.name"),
            getBundle().getString("app.info_services_systemd.label.status"),
            getBundle().getString("app.info_services_systemd.label.state"),
            getBundle().getString("app.info_services_systemd.label.description")
    };
    private final List<ServiceEntry> list = new ArrayList<>();

    public void addEntry(ServiceEntry e) {
        list.add(e);
        fireTableDataChanged();
    }

    public void addEntries(List<ServiceEntry> entries) {
        if (entries != null) {
            list.addAll(entries);
            fireTableDataChanged();
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ServiceEntry e = list.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return e.getName();
            case 1:
                return e.getUnitFileStatus();
            case 2:
                return e.getUnitStatus();
            case 3:
                return e.getDesc();
            default:
                return "";
        }
    }

    public void clear() {
        list.clear();
    }
}
