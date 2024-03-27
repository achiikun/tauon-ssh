/**
 *
 */
package tauon.app.ui.containers.session.pages.utilpage.nettools;

import tauon.app.App;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextArea;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.utilpage.UtilPageItemView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class NetworkToolsPage extends UtilPageItemView {
    private JTextArea txtOutput;
    private DefaultComboBoxModel<String> modelHost, modelPort;
    private JComboBox<String> cmbHost, cmbPort, cmbDNSTool;
    private JButton btn1, btn2, btn3, btn4;

    /**
     *
     */
    public NetworkToolsPage(SessionContentPanel holder) {
        super(holder);
    }

    @Override
    protected void createUI() {
        modelHost = new DefaultComboBoxModel<String>();
        modelPort = new DefaultComboBoxModel<String>();

        cmbHost = new JComboBox<String>(modelHost);
        cmbPort = new JComboBox<String>(modelPort);
        cmbHost.setEditable(true);
        cmbPort.setEditable(true);

        cmbDNSTool = new JComboBox<String>(new String[]{"nslookup", "dig",
                "dig +short", "host", "getent ahostsv4"});

        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 10));
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));

        btn1 = new JButton("Ping");
        btn2 = new JButton("Port check");
        btn3 = new JButton("Traceroute");
        btn4 = new JButton("DNS lookup");

        btn1.addActionListener(e -> {
            if (JOptionPane.showOptionDialog(this,
                    new Object[]{getBundle().getString("host_ping"), cmbHost}, "Ping",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, null, null) == JOptionPane.OK_OPTION) {
                executeAsync("ping -c 4 " + cmbHost.getSelectedItem());
            }
        });

        btn2.addActionListener(e -> {
            if (JOptionPane.showOptionDialog(this,
                    new Object[]{getBundle().getString("host_name"), cmbHost, getBundle().getString("port_number"),
                            cmbPort},
                    "Port check", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.OK_OPTION) {
                executeAsync("bash -c 'test cat</dev/tcp/"
                        + cmbHost.getSelectedItem() + "/"
                        + cmbPort.getSelectedItem()
                        + " && echo \"Port Reachable\" || echo \"Port Not reachable\"'");
            }
        });

        btn3.addActionListener(e -> {
            if (JOptionPane.showOptionDialog(this,
                    new Object[]{getBundle().getString("host_name"), cmbHost}, "Traceroute",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, null, null) == JOptionPane.OK_OPTION) {
                executeAsync("traceroute " + cmbHost.getSelectedItem());
            }
        });

        btn4.addActionListener(e -> {
            if (JOptionPane.showOptionDialog(this,
                    new Object[]{getBundle().getString("host_name"), cmbHost, getBundle().getString("tool_use"),
                            cmbDNSTool},
                    "DNS lookup", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.OK_OPTION) {
                executeAsync(cmbDNSTool.getSelectedItem() + " "
                        + cmbHost.getSelectedItem());
            }
        });

        grid.add(btn1);
        grid.add(btn2);
        grid.add(btn3);
        grid.add(btn4);

        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.add(grid, BorderLayout.NORTH);

        txtOutput = new SkinnedTextArea();
        txtOutput.setEditable(false);
        JScrollPane jsp = new SkinnedScrollPane(txtOutput);
        jsp.setBorder(new LineBorder(App.skin.getDefaultBorderColor()));
        this.add(jsp);
    }

    private void executeAsync(String cmd) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
//        holder.disableUi(stopFlag);
        holder.submitSSHOperationStoppable(instance -> {
            StringBuilder outText = new StringBuilder();
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ByteArrayOutputStream berr = new ByteArrayOutputStream();
                if (instance.execBin(cmd, stopFlag,
                        bout, berr) == 0) {
                    outText.append(bout.toString(StandardCharsets.UTF_8)).append("\n");
                    System.out.println("Command stdout: " + outText);
                } else {
                    System.err.println(berr.toString(StandardCharsets.UTF_8));
                    JOptionPane.showMessageDialog(this,
                            getBundle().getString("executed_errors"));
                }
            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
            finally {
                SwingUtilities.invokeLater(() -> {
                    this.txtOutput.setText(outText.toString());
                });
//                holder.enableUi();
            }
        }, stopFlag);
    }

    @Override
    protected void onComponentVisible() {

    }

    @Override
    protected void onComponentHide() {

    }
}
