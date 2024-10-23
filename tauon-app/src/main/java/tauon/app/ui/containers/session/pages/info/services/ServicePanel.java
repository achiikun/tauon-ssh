/**
 *
 */
package tauon.app.ui.containers.session.pages.info.services;

import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;
import tauon.app.util.ssh.SudoUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 */
public class ServicePanel extends Subpage {
    private static final Pattern SERVICE_PATTERN = Pattern
            .compile("(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+([\\S]+.*)");
    private static final Pattern UNIT_PATTERN = Pattern
            .compile("(\\S+)\\s+([\\S]+.*)");
    private static final String SEP = UUID.randomUUID().toString();
    public static final String SYSTEMD_COMMAND = "systemctl list-unit-files -t service -a "
            + "--plain --no-pager --no-legend --full; echo " + SEP
            + "; systemctl list-units -t service -a --plain --no-pager --no-legend --full";
    private final ServiceTableModel model = new ServiceTableModel();
    private JTable table;
    private JButton btnStart, btnStop, btnRestart, btnReload, btnEnable,
            btnDisable, btnRefresh;
    private JTextField txtFilter;
    private JCheckBox chkRunAsSuperUser;
    private JButton btnFilter;
    private List<ServiceEntry> list;

    /**
     *
     */
    public ServicePanel(SessionContentPanel holder) {
        super(holder);
    }

    private static List<ServiceEntry> parseServiceEntries(StringBuilder data) {
        List<ServiceEntry> list = new ArrayList<>();
        Map<String, String> unitMap = new HashMap<>();
        boolean parsingUnit = true;
        for (String s : data.toString().split("\n")) {
            if (parsingUnit && s.equals(SEP)) {
                parsingUnit = false;
                continue;
            }

            if (parsingUnit) {
                parseUnitFile(s, unitMap);
            } else {
                ServiceEntry ent = parseUnit(s, unitMap);
                if (ent != null) {
                    list.add(ent);
                }
            }
        }

        return list;
    }


    private static void parseUnitFile(String data, Map<String, String> map) {
        Matcher m = UNIT_PATTERN.matcher(data);
        if (m.find() && m.groupCount() == 2) {
            map.put(m.group(1).trim(), m.group(2).trim());
        }
    }

    private static ServiceEntry parseUnit(String data,
                                          Map<String, String> unitMap) {
        ServiceEntry ent = new ServiceEntry();
        Matcher m = SERVICE_PATTERN.matcher(data);
        if (m.find() && m.groupCount() == 5) {
            String name = m.group(1).trim();
            if (unitMap.get(name) != null) {
                String status = unitMap.get(name);
                ent.setName(name);
                ent.setUnitFileStatus(status);
                ent.setUnitStatus(m.group(3) + "(" + m.group(4) + ")");
                ent.setDesc(m.group(5).trim());
                return ent;
            }

        }
        return null;
    }

    public void setElevationActionListener(ActionListener a) {
        chkRunAsSuperUser.addActionListener(a);
    }

    public void setStartServiceActionListener(ActionListener a) {
        btnStart.addActionListener(a);
    }

    public void setStopServiceActionListener(ActionListener a) {
        btnStop.addActionListener(a);
    }

    public void setRestartServiceActionListener(ActionListener a) {
        btnRestart.addActionListener(a);
    }

    public void setReloadServiceActionListener(ActionListener a) {
        btnReload.addActionListener(a);
    }

    public void setEnableServiceActionListener(ActionListener a) {
        btnEnable.addActionListener(a);
    }

    public void setDisableServiceActionListener(ActionListener a) {
        btnDisable.addActionListener(a);
    }

    private void filter() {
        String text = txtFilter.getText();
        model.clear();
        if (text.length() > 0) {
            List<ServiceEntry> filteredList = new ArrayList<>();
            for (ServiceEntry entry : list) {
                if (entry.getName().contains(text)
                        || entry.getDesc().contains(text)
                        || entry.getUnitStatus().contains(text)) {
                    filteredList.add(entry);
                }
            }
            model.addEntries(filteredList);
        } else {
            model.addEntries(list);
        }
    }

    private String getSelectedService() {
        int r = table.getSelectedRow();
        if (r < 0) {
            return null;
        }
        return (String) model.getValueAt(table.convertRowIndexToModel(r), 0);
    }

    public String getStartServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl start " + cmd;
    }

    public String getStopServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl stop " + cmd;
    }

    public String getRestartServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl restart " + cmd;
    }

    public String getReloadServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl reload " + cmd;
    }

    public String getEnableServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl enable " + cmd;
    }

    public String getDisableServiceCommand() {
        String cmd = getSelectedService();
        if (cmd == null) {
            return null;
        }
        return "systemctl disable " + cmd;
    }

    public boolean getUseSuperUser() {
        return chkRunAsSuperUser.isSelected();
    }

    public void setUseSuperUser(boolean select) {
        chkRunAsSuperUser.setSelected(select);
    }

    private void setServiceData(List<ServiceEntry> list) {
        this.list = list;
        filter();
    }

    @Override
    protected void createUI() {
        setBorder(new EmptyBorder(10, 10, 10, 10));

        ServiceTableCellRenderer r = new ServiceTableCellRenderer();

        table = new JTable(model);
        table.setDefaultRenderer(Object.class, r);
        table.setShowGrid(false);
        table.setRowHeight(r.getPreferredSize().height);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        JLabel lbl1 = new JLabel(getBundle().getString("app.info_services_systemd.action.search"));
        txtFilter = new SkinnedTextField(30);
        txtFilter.addActionListener(e -> filter());
        btnFilter = new JButton(getBundle().getString("app.info_services_systemd.action.search"));

        Box b1 = Box.createHorizontalBox();
        b1.add(lbl1);
        b1.add(Box.createHorizontalStrut(5));
        b1.add(txtFilter);
        b1.add(Box.createHorizontalStrut(5));
        b1.add(btnFilter);

        add(b1, BorderLayout.NORTH);

        btnFilter.addActionListener(e -> filter());
        table.setAutoCreateRowSorter(true);
        add(new SkinnedScrollPane(table));

        Box box = Box.createHorizontalBox();

        btnStart = new JButton(getBundle().getString("app.info_services_systemd.action.start"));
        btnStop = new JButton(getBundle().getString("app.info_services_systemd.action.stop"));
        btnRestart = new JButton(getBundle().getString("app.info_services_systemd.action.restart"));
        btnReload = new JButton(getBundle().getString("app.info_services_systemd.action.reload"));
        btnEnable = new JButton(getBundle().getString("app.info_services_systemd.action.enable"));
        btnDisable = new JButton(getBundle().getString("app.info_services_systemd.action.disable"));
        btnRefresh = new JButton(getBundle().getString("app.info_services_systemd.action.refresh"));

        chkRunAsSuperUser = new JCheckBox(
                getBundle().getString("app.ui.action.do_using_sudo"));
        box.add(chkRunAsSuperUser);

        box.add(Box.createHorizontalGlue());
        box.add(btnStart);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnStop);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnRestart);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnReload);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnEnable);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnDisable);
        box.add(Box.createHorizontalStrut(5));
        box.add(btnRefresh);
        box.add(Box.createHorizontalStrut(5));
        box.setBorder(new EmptyBorder(10, 0, 0, 0));

        add(box, BorderLayout.SOUTH);

        this.setStartServiceActionListener(e -> performServiceAction(1));
        this.setStopServiceActionListener(e -> performServiceAction(2));
        this.setEnableServiceActionListener(e -> performServiceAction(3));
        this.setDisableServiceActionListener(e -> performServiceAction(4));
        this.setReloadServiceActionListener(e -> performServiceAction(5));
        this.setRestartServiceActionListener(e -> performServiceAction(6));

        btnRefresh.addActionListener(e -> {
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            holder.submitSSHOperationStoppable(instance -> {
                updateView(instance, stopFlag);
            }, stopFlag);
        });
//                holder.executor.submit(() -> {
//                    holder.disableUi(stopFlag);
//                    holder.enableUi();
//                }));

//        holder.executor.submit(() -> {
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            holder.submitSSHOperationStoppable(instance -> {
                updateView(instance, stopFlag);
            }, stopFlag);
//        });
    }

    @Override
    protected void onComponentVisible() {

    }

    @Override
    protected void onComponentHide() {

    }

    private void performServiceAction(int option) {
        String cmd1 = null;
        switch (option) {
            case 1:
                cmd1 = this.getStartServiceCommand();
                break;
            case 2:
                cmd1 = this.getStopServiceCommand();
                break;
            case 3:
                cmd1 = this.getEnableServiceCommand();
                break;
            case 4:
                cmd1 = this.getDisableServiceCommand();
                break;
            case 5:
                cmd1 = this.getReloadServiceCommand();
                break;
            case 6:
                cmd1 = this.getRestartServiceCommand();
                break;
        }

        String cmd = cmd1;

        AtomicBoolean stopFlag = new AtomicBoolean(false);

//        holder.disableUi(stopFlag);

        boolean elevated = this.getUseSuperUser();
        if (cmd != null) {
            holder.submitSSHOperationStoppable(instance -> {
//                try {
                    if (elevated) {
//                        try {
                            if (this.runCommandWithSudo(instance, stopFlag,
                                    cmd, holder.getInfo().getPassword())) {
                                updateView(instance, stopFlag);
                                return;
                            }
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
                        if (!holder.isSessionClosed()) {
                            JOptionPane.showMessageDialog(null,
                                    getBundle().getString("general.message.operation_failed"));
                        }
                    } else {
//                        try {
                            if (this.runCommand(instance, stopFlag,
                                    cmd)) {
                                updateView(instance, stopFlag);
                                return;
                            }
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
                        if (!holder.isSessionClosed()) {
                            JOptionPane.showMessageDialog(null,
                                    getBundle().getString("general.message.operation_failed"));
                        }
                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    holder.enableUi();
//                }
            }, stopFlag);
        }
    }

    public boolean runCommandWithSudo(TauonRemoteSessionInstance client,
                                      AtomicBoolean stopFlag, String command, String password) throws Exception {
        return SudoUtils.runSudo(command, client, password) == 0;
    }

    public boolean runCommand(TauonRemoteSessionInstance client,
                              AtomicBoolean stopFlag, String command) throws Exception {
        StringBuilder output = new StringBuilder();
        return client.exec(command, new AtomicBoolean(false), output) == 0;
    }

    private void updateView(TauonRemoteSessionInstance instance, AtomicBoolean stopFlag) throws Exception {
//        try {
            StringBuilder output = new StringBuilder();
            int ret = instance.exec(SYSTEMD_COMMAND,
                    stopFlag, output);
            if (ret == 0) {
                List<ServiceEntry> list = ServicePanel
                        .parseServiceEntries(output);
                // TODO test invoke later
                SwingUtilities.invokeAndWait(() -> setServiceData(list));
            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
