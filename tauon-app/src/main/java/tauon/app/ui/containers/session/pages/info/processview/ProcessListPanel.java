package tauon.app.ui.containers.session.pages.info.processview;

import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.components.tablerenderers.ByteCountRenderer;
import tauon.app.ui.components.tablerenderers.ByteCountValue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import static tauon.app.services.LanguageService.getBundle;

public class ProcessListPanel extends JPanel {
    private final ProcessTableModel model;
    private final JTable table;
    private final JTextField txtFilter;
    private final RowFilter<ProcessTableModel, Integer> rowFilter;
    private final JPopupMenu killPopup;
    private final JPopupMenu prioPopup;
    private final BiConsumer<String, CommandMode> consumer;
    private final JLabel lblProcessCount;
    private JButton btnKill;
    private final JButton btnCopyArgs;
    private String filterText = "";

    public ProcessListPanel(BiConsumer<String, CommandMode> consumer) {
        super(new BorderLayout());
        this.consumer = consumer;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel pan = new JPanel(new BorderLayout(5, 5));
        model = new ProcessTableModel();
        table = new JTable(model);

        table.getSelectionModel().addListSelectionListener(e -> enableProcessesButtons());
        
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        ByteCountRenderer byteCountRenderer = new ByteCountRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(ByteCountValue.class, byteCountRenderer);
        table.setRowHeight(Math.max(
                renderer.getPreferredSize().height,
                byteCountRenderer.getPreferredSize().height
        ));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);

        lblProcessCount = new JLabel(getBundle().getString("app.info_processes.label.total_processes") + " 0");

        rowFilter = new RowFilter<ProcessTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends ProcessTableModel, ? extends Integer> entry) {
                if (filterText.length() < 1) {
                    return true;
                }
                Integer index = entry.getIdentifier();
                int c = entry.getModel().getColumnCount();
                for (int i = 0; i < c; i++) {
                    if ((entry.getModel().getValueAt(index, i)).toString()
                            .toLowerCase(Locale.ENGLISH)
                            .contains(filterText.toLowerCase(Locale.ENGLISH))) {
                        return true;
                    }
                }
                return false;
            }
        };

        TableRowSorter<ProcessTableModel> sorter = new TableRowSorter<>(
                model);
        sorter.setRowFilter(rowFilter);
        table.setRowSorter(sorter);

        JScrollPane jsp = new SkinnedScrollPane(table);
        pan.add(jsp);

        Box b1 = Box.createHorizontalBox();
        b1.add(new JLabel(getBundle().getString("app.info_processes.label.processes")));
        b1.add(Box.createHorizontalStrut(10));
        txtFilter = new SkinnedTextField(30);
        txtFilter.addActionListener(e -> {
            this.filterText = getProcessFilterText();
            model.fireTableDataChanged();
        });
        b1.add(txtFilter);
        b1.add(Box.createHorizontalStrut(5));
        JButton btnFilter = new JButton(getBundle().getString("app.info_processes.action.filter"));
        btnFilter.addActionListener(e -> {
            this.filterText = getProcessFilterText();
            model.fireTableDataChanged();
        });
        b1.add(btnFilter);
        b1.add(Box.createHorizontalStrut(5));

        JButton btnClearFilter = new JButton(getBundle().getString("app.info_processes.action.clear"));
        b1.add(btnClearFilter);
        b1.add(Box.createHorizontalStrut(5));
        btnClearFilter.addActionListener(e -> {
            this.txtFilter.setText("");
            this.filterText = getProcessFilterText();
            model.fireTableDataChanged();
        });

        JButton btnRefresh = new JButton(getBundle().getString("general.action.refresh"));
        b1.add(btnRefresh);
        btnRefresh.addActionListener(e -> this.consumer.accept(null, CommandMode.LIST_PROCESS));

        killPopup = new JPopupMenu();

        JMenuItem mKill = new JMenuItem(getBundle().getString("app.info_processes.action.kill"));
        JMenuItem mKillAsRoot = new JMenuItem(getBundle().getString("app.info_processes.action.kill_sudo"));

        mKill.addActionListener(e -> {
            int c = table.getSelectedRow();
            if (c != -1) {

                btnKill.setEnabled(false);
                ProcessTableEntry ent = model
                        .get(table.convertRowIndexToModel(c));
                this.consumer.accept("kill -9 " + ent.getPid(),
                        CommandMode.KILL_AS_USER);
                enableProcessesButtons();
            }
        });

        mKillAsRoot.addActionListener(e -> {
            int c = table.getSelectedRow();
            if (c != -1) {

                ProcessTableEntry ent = model
                        .get(table.convertRowIndexToModel(c));
                btnKill.setEnabled(false);
                this.consumer.accept("kill -9 " + ent.getPid(),
                        CommandMode.KILL_AS_ROOT);
                enableProcessesButtons();
            }
        });

        killPopup.add(mKill);
        killPopup.add(mKillAsRoot);

        killPopup.pack();

        prioPopup = new JPopupMenu();
        JMenuItem mPrio = new JMenuItem(getBundle().getString("app.info_processes.action.change_priority"));
        JMenuItem mPrioAsRoot = new JMenuItem(getBundle().getString("app.info_processes.action.change_priority_sudo"));
        prioPopup.add(mPrio);
        prioPopup.add(mPrioAsRoot);
        prioPopup.pack();


        Box b2 = Box.createHorizontalBox();
        b2.add(lblProcessCount);
        btnCopyArgs = new JButton(getBundle().getString("app.info_processes.action.copy_command"));
        btnCopyArgs.addActionListener(e -> {
            int c = table.getSelectedRow();
            if (c != -1) {
                ProcessTableEntry ent = model
                        .get(table.convertRowIndexToModel(c));
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(ent.getArgs()), null);
            }
        });

        btnKill = new JButton(getBundle().getString("app.info_processes.action.kill_process"));
        btnKill.addActionListener(e -> {
            Dimension d = killPopup.getPreferredSize();
            killPopup.show(btnKill, 0, -d.height);
        });
        b2.add(Box.createHorizontalGlue());
        b2.add(btnCopyArgs);
        b2.add(Box.createHorizontalStrut(5));
        b2.add(btnKill);
        b2.add(Box.createHorizontalStrut(5));

        pan.add(b1, BorderLayout.NORTH);
        pan.add(b2, BorderLayout.SOUTH);
        add(pan);

        btnKill.setEnabled(false);
        btnCopyArgs.setEnabled(false);
    }

    public String getProcessFilterText() {
        return txtFilter.getText();
    }

    public void setProcessList(List<ProcessTableEntry> list) {
        lblProcessCount.setText(getBundle().getString("app.info_processes.label.total_processes") + " " + list.size()
                + ", " + getBundle().getString("app.info_processes.label.last_updated") + " " + LocalDateTime.now().format(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));
        int x = table.getSelectedRow();
        int selectedPid = -1;
        if (x != -1) {
            int xc = table.convertRowIndexToModel(x);
            selectedPid = this.model.get(xc).getPid();
        }
        this.model.setProcessList(list);
        if (selectedPid != -1) {
            int i = 0;
            for (ProcessTableEntry ent : this.model.getProcessList()) {
                if (ent.getPid() == selectedPid) {
                    int r = table.convertRowIndexToView(i);
                    table.setRowSelectionInterval(r, r);
                    break;
                }
                i++;
            }
        }
    }

    public enum CommandMode {
        KILL_AS_ROOT, KILL_AS_USER, LIST_PROCESS
    }

    private void enableProcessesButtons(){
        btnKill.setEnabled(table.getSelectedRows().length > 0);
        btnCopyArgs.setEnabled(table.getSelectedRows().length > 0);
    }
}
