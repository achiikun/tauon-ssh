package tauon.app.ui.dialogs.sessions;

import tauon.app.App;
import tauon.app.settings.PortForwardingRule;
import tauon.app.settings.PortForwardingRule.PortForwardingType;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

public class PortForwardingPanel extends JPanel {
    private final PFTableModel model;
    private final JTable table;
    private SessionInfo info;

    public PortForwardingPanel() {
        super(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 0, 0, 10));
        model = new PFTableModel();
        table = new JTable(model);

        JLabel lblTitle = new JLabel("Port forwarding rules");

        JScrollPane scrollPane = new SkinnedScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        Box b1 = Box.createVerticalBox();
        JButton btnAdd = new JButton(FontAwesomeContants.FA_PLUS);
        btnAdd.setFont(App.skin.getIconFont());
        JButton btnDel = new JButton(FontAwesomeContants.FA_MINUS);
        btnDel.setFont(App.skin.getIconFont());
        JButton btnEdit = new JButton(FontAwesomeContants.FA_PENCIL);
        btnEdit.setFont(App.skin.getIconFont());

        btnAdd.addActionListener(e -> {
            PortForwardingRule ent = addOrEditEntry(null);
            if (ent != null) {
                model.addRule(ent);
                updatePFRules();
            }
        });

        btnEdit.addActionListener(e -> {
            int index = table.getSelectedRow();
            if (index != -1) {
                PortForwardingRule ent = model.get(index);
                if (addOrEditEntry(ent) != null) {
                    model.refreshTable();
                    updatePFRules();
                }
            }
        });

        btnDel.addActionListener(e -> {
            int index = table.getSelectedRow();
            if (index != -1) {
                model.remove(index);
                updatePFRules();
            }
        });

        b1.add(btnAdd);
        b1.add(Box.createVerticalStrut(10));

        b1.add(btnEdit);
        b1.add(Box.createVerticalStrut(10));

        b1.add(btnDel);
        b1.add(Box.createVerticalStrut(10));

        this.add(lblTitle, BorderLayout.NORTH);
        this.add(scrollPane);
        this.add(b1, BorderLayout.EAST);
    }

    private void updatePFRules() {
        this.info.setPortForwardingRules(model.getRules());
    }

    public void setInfo(SessionInfo info) {
        this.info = info;
        model.setRules(this.info.getPortForwardingRules());
    }

    private PortForwardingRule addOrEditEntry(PortForwardingRule r) {
        JComboBox<String> cmbPFType = new JComboBox<>(new String[]{
                getBundle().getString("local"),
                getBundle().getString("remote")
        });
        
        JTextField txtLocalHost = new SkinnedTextField(30);
        txtLocalHost.setText("127.0.0.1");
        
        JSpinner spLocalPort = new JSpinner(new SpinnerNumberModel(0, 0, SessionInfoPanel.DEFAULT_MAX_PORT, 1));

        JTextField txtRemoteHost = new SkinnedTextField(30);
        txtRemoteHost.setText("127.0.0.1");

        JSpinner spRemotePort = new JSpinner(new SpinnerNumberModel(0, 0, SessionInfoPanel.DEFAULT_MAX_PORT, 1));

        if (r != null) {
            txtLocalHost.setText(r.getLocalHost());
            spLocalPort.setValue(r.getLocalPort());
            txtRemoteHost.setText(r.getRemoteHost());
            spRemotePort.setValue(r.getRemotePort());
            cmbPFType.setSelectedIndex(r.getType() == PortForwardingType.Local ? 0 : 1);
        }

        while (JOptionPane.showOptionDialog(this,
                // TODO i18n
                new Object[]{
                        "Port forwarding type", cmbPFType,
                        "Local Host", txtLocalHost,
                        "Local Port", spLocalPort,
                        "Remote Host", txtRemoteHost,
                        "Remote Port", spRemotePort,
                },
                "Port forwarding rule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.OK_OPTION) {

            String host = txtRemoteHost.getText();
            int port1 = (Integer) spRemotePort.getValue();
            int port2 = (Integer) spLocalPort.getValue();
            String bindAddress = txtLocalHost.getText();

            if (host.isEmpty() || bindAddress.isEmpty() || port1 <= 0 || port2 <= 0) {
                JOptionPane.showMessageDialog(this, getBundle().getString("invalid_input"));
                continue;
            }

            if (r == null) {
                r = new PortForwardingRule();
            }
            r.setType(cmbPFType.getSelectedIndex() == 0 ? PortForwardingType.Local : PortForwardingType.Remote);
            r.setRemoteHost(host);
            r.setLocalHost(bindAddress);
            r.setRemotePort(port1);
            r.setLocalPort(port2);
            return r;
        }
        return null;
    }

    private static class PFTableModel extends AbstractTableModel {

        private final String[] columns = {
                getBundle().getString("type"),
                getBundle().getString("local_host"),
                getBundle().getString("local_port"),
                getBundle().getString("remote_host"),
                getBundle().getString("remote_port")
        };
        
        private final List<PortForwardingRule> list = new ArrayList<>();

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
            PortForwardingRule pf = list.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return pf.getType();
                case 1:
                    return pf.getLocalHost();
                case 2:
                    return pf.getLocalPort();
                case 3:
                    return pf.getRemoteHost();
                case 4:
                    return pf.getRemotePort();
                default:
                    return "";
            }
        }

        private List<PortForwardingRule> getRules() {
            return list;
        }

        private void setRules(List<PortForwardingRule> rules) {
            list.clear();
            if (rules != null) {
                list.addAll(rules);
            }
            fireTableDataChanged();
        }

        private void addRule(PortForwardingRule r) {
            this.list.add(r);
            fireTableDataChanged();
        }

        private void refreshTable() {
            fireTableDataChanged();
        }

        private void remove(int index) {
            list.remove(index);
            fireTableDataChanged();
        }

        private PortForwardingRule get(int index) {
            return list.get(index);
        }
    }
}
