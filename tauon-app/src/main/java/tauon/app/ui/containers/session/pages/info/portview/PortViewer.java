/**
 *
 */
package tauon.app.ui.containers.session.pages.info.portview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.ssh.IStopper;
import tauon.app.ssh.SSHCommandRunner;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 */
public class PortViewer extends Subpage {
    private static final Logger LOG = LoggerFactory.getLogger(PortViewer.class);
    
    private static final String SEPARATOR = UUID.randomUUID().toString();
    public static final String LSOF_COMMAND = "sh -c \"export PATH=$PATH:/usr/sbin; echo;echo "
            + SEPARATOR + ";lsof -b -n -i tcp -P -s tcp:LISTEN -F cn 2>&1\"";
    private final SocketTableModel model = new SocketTableModel();
    private JTable table;
    private JButton btnRefresh;
    private JTextField txtFilter;
    private JCheckBox chkRunAsSuperUser;
    private JButton btnFilter;
    private List<SocketEntry> list;
    
    /**
     *
     */
    public PortViewer(SessionContentPanel holder) {
        super(holder);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
    }
    
    private void filter() {
        String text = txtFilter.getText();
        model.clear();
        if (!text.isEmpty()) {
            List<SocketEntry> filteredList = new ArrayList<>();
            for (SocketEntry entry : list) {
                if (entry.getApp().contains(text)
                        || (entry.getPort() + "").contains(text)
                        || entry.getHost().contains(text)
                        || (entry.getPid() + "").contains(text)) {
                    filteredList.add(entry);
                }
            }
            model.addEntries(filteredList);
        } else {
            model.addEntries(list);
        }
        model.fireTableDataChanged();
    }
    
    public boolean getUseSuperUser() {
        return chkRunAsSuperUser.isSelected();
    }
    
    public List<SocketEntry> parseSocketList(String text) {
        System.err.println("text: " + text);
        List<SocketEntry> list = new ArrayList<>();
        SocketEntry ent = null;
        boolean start = false;
        for (String line1 : text.split("\n")) {
            String line = line1.trim();
            System.out.println("LINE=" + line);
            if (!start) {
                if (line.trim().equals(SEPARATOR)) {
                    start = true;
                }
                continue;
            }
            char ch = line.charAt(0);
            if (ch == 'p') {
                if (ent != null) {
                    list.add(ent);
                }
                ent = new SocketEntry();
                ent.setPid(Integer.parseInt(line.substring(1)));
            }
            if (ch == 'c') {
                ent.setApp(line.substring(1));
            }
            if (ch == 'n') {
                String hostStr = line.substring(1);
                int index = hostStr.lastIndexOf(":");
                if (index != -1) {
                    int port = Integer.parseInt(hostStr.substring(index + 1));
                    String host = hostStr.substring(0, index);
                    if (ent.getHost() != null) {
                        // if listening on multiple interfaces, ports
                        SocketEntry ent1 = new SocketEntry();
                        ent1.setPort(port);
                        ent1.setHost(host);
                        ent1.setApp(ent.getApp());
                        ent1.setPid(ent.getPid());
                        list.add(ent1);
                    } else {
                        ent.setPort(port);
                        ent.setHost(host);
                    }
                }
            }
        }
        if (ent != null) {
            list.add(ent);
        }
        return list;
    }
    
    public void setSocketData(List<SocketEntry> list) {
        this.list = list;
        filter();
    }
    
    @Override
    protected void createUI() {
        table = new JTable(model);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        
        JLabel lbl1 = new JLabel(getBundle().getString("app.info_ports.action.search"));
        txtFilter = new SkinnedTextField(30);
        btnFilter = new JButton(getBundle().getString("app.info_ports.action.search"));
        
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
        box.setBorder(new EmptyBorder(10, 0, 0, 0));
        btnRefresh = new JButton(getBundle().getString("general.action.refresh"));
        btnRefresh.addActionListener(e -> getListingSockets());
        
        chkRunAsSuperUser = new JCheckBox(
                getBundle().getString("app.ui.action.do_using_sudo"));
        box.add(chkRunAsSuperUser);
        
        box.add(Box.createHorizontalGlue());
        box.add(btnRefresh);
        box.add(Box.createHorizontalStrut(5));
        
        add(box, BorderLayout.SOUTH);
        
        getListingSockets();
    }
    
    @Override
    protected void onComponentVisible() {
    
    }
    
    @Override
    protected void onComponentHide() {
    
    }
    
    private void getListingSockets() {
        IStopper.Handle stopFlag = new IStopper.Default();
        
        boolean elevated = this.getUseSuperUser();
        holder.submitSSHOperationStoppable((guiHandle, instance) -> {
            
            SSHCommandRunner sshCommandRunner = new SSHCommandRunner()
                    .withCommand(LSOF_COMMAND)
                    .withStdoutString()
                    .withStopper(stopFlag)
                    .withSudo(elevated ? guiHandle : null);
            
            instance.exec(sshCommandRunner);
            
            if (sshCommandRunner.getResult() == 0) {
                java.util.List<SocketEntry> list = this.parseSocketList(sshCommandRunner.getStdoutString());
                try {
                    SwingUtilities.invokeAndWait(() -> setSocketData(list));
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new AlreadyFailedException();
                }
            } else {
                LOG.warn("Lsof command returned an error. Clearing list.");
                try {
                    SwingUtilities.invokeAndWait(() -> setSocketData(Collections.emptyList()));
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new AlreadyFailedException();
                }
            }
            
        }, stopFlag);
//        }
    }
}
