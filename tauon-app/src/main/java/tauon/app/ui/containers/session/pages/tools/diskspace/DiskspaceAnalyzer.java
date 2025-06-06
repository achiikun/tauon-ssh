/**
 *
 */
package tauon.app.ui.containers.session.pages.tools.diskspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.ssh.IStopper;
import tauon.app.ssh.SSHCommandRunner;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.page.subpage.Subpage;
import tauon.app.ui.components.tablerenderers.ByteCountRenderer;
import tauon.app.ui.components.tablerenderers.ByteCountValue;
import tauon.app.ui.components.tablerenderers.PercentageRenderer;
import tauon.app.ui.components.tablerenderers.PercentageValue;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.OptionPaneUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class DiskspaceAnalyzer extends Subpage {
    private static final Logger LOG = LoggerFactory.getLogger(DiskspaceAnalyzer.class);
    
    private final CardLayout cardLayout;
    private PartitionTableModel model;
    private JTable table;
    private JTree resultTree;
    private DefaultTreeModel treeModel;
    private JCheckBox chkRunAsSuperUser1;
    private JCheckBox chkRunAsSuperUser2;
    
    private String lastAnalyzedPath;
    
    /**
     *
     */
    public DiskspaceAnalyzer(SessionContentPanel holder) {
        super(holder);
        Component firstPanel = createFirstPanel();
        
        chkRunAsSuperUser1 = new JCheckBox(
                getBundle().getString("app.ui.action.do_using_sudo"));
        chkRunAsSuperUser2 = new JCheckBox(
                getBundle().getString("app.ui.action.do_using_sudo"));
        chkRunAsSuperUser1.addChangeListener(changeEvent -> chkRunAsSuperUser2.setSelected(chkRunAsSuperUser1.isSelected()));
        chkRunAsSuperUser2.addChangeListener(changeEvent -> chkRunAsSuperUser1.setSelected(chkRunAsSuperUser2.isSelected()));
        
        cardLayout = new CardLayout();
        this.setLayout(cardLayout);
        this.add(firstPanel, "firstPanel");
        this.add(createVolumesPanel(), "volPanel");
        this.add(createResultPanel(), "resultPanel");
    }

    private Component createResultPanel() {
        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("results", true), true);
        resultTree = new JTree(treeModel);
        resultTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        JButton btnStart = new JButton(getBundle().getString("app.tools_diskspace.action.start_another_analysis"));
        btnStart.addActionListener(e -> {
            cardLayout.show(this, "firstPanel");
        });
        JButton btnReload = new JButton(getBundle().getString("general.action.reload"));
        btnReload.addActionListener(e -> {
            if(lastAnalyzedPath != null){
                analyze(lastAnalyzedPath);
            }
        });

        Box resultBox = Box.createHorizontalBox();
        resultBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        resultBox.add(chkRunAsSuperUser1);
        resultBox.add(Box.createHorizontalStrut(10));
        resultBox.add(btnReload);
        resultBox.add(Box.createHorizontalGlue());
        resultBox.add(Box.createHorizontalStrut(10));
        resultBox.add(btnStart);

        JLabel resultTitle = new JLabel(getBundle().getString("app.tools_diskspace.label.directory_usage"));
        resultTitle.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.add(resultBox, BorderLayout.SOUTH);
        resultPanel.add(new SkinnedScrollPane(resultTree));
        resultPanel.add(resultTitle, BorderLayout.NORTH);
        return resultPanel;
    }

    private Component createFirstPanel() {
        JRadioButton radFolder = new JRadioButton(getBundle().getString("app.tools_diskspace.label.analyze_folder"));
        JRadioButton radVolume = new JRadioButton(getBundle().getString("app.tools_diskspace.label.analyze_volume"));
        radFolder.setFont(App.skin.getDefaultFont().deriveFont(Constants.SMALL_TEXT_SIZE));
        radVolume.setFont(App.skin.getDefaultFont().deriveFont(Constants.SMALL_TEXT_SIZE));
        radFolder.setHorizontalAlignment(JRadioButton.LEFT);
        radVolume.setHorizontalAlignment(JRadioButton.LEFT);
        JLabel lblIcon = new JLabel();
        lblIcon.setFont(App.skin.getIconFont().deriveFont(128.0f));
        lblIcon.setText(FontAwesomeContants.FA_HDD_O);
        JButton btnNext = new JButton(getBundle().getString("general.action.next"));
        btnNext.addActionListener(e -> {
            if (radVolume.isSelected()) {
                cardLayout.show(this, "volPanel");
                listVolumes();
            } else {
                String text = OptionPaneUtils.showInputDialog(this, getBundle().getString("app.tools_diskspace.action.enter_folder_to_analyze"), getBundle().getString("general.action.input"));
                if (text != null) {
                    cardLayout.show(this, "resultPanel");
                    analyze(text);
                }
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(radVolume);
        bg.add(radFolder);

        radVolume.setSelected(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.ipadx = 20;
        gc.gridheight = 3;
        panel.add(lblIcon, gc);
        gc.ipadx = 20;
        gc.ipady = 10;
        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridheight = 1;
        panel.add(radVolume, gc);
        gc.gridx = 1;
        gc.gridy = 1;
        panel.add(radFolder, gc);
        gc.gridx = 1;
        gc.gridy = 2;
        gc.ipadx = 20;
        panel.add(btnNext, gc);
        return panel;
    }

    private Component createVolumesPanel() {
        ByteCountRenderer byteCountRenderer = new ByteCountRenderer();
        PercentageRenderer percentageRenderer = new PercentageRenderer();
        
        model = new PartitionTableModel();
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer());
        table.setDefaultRenderer(ByteCountValue.class, byteCountRenderer);
        table.setDefaultRenderer(PercentageValue.class, percentageRenderer);
        table.setRowHeight(Math.max(byteCountRenderer.getPreferredSize().height, percentageRenderer.getPreferredSize().height));
        table.setSelectionForeground(App.skin.getDefaultSelectionForeground());
        JScrollPane jsp = new SkinnedScrollPane(table);

        JButton btnBack = new JButton(getBundle().getString("general.action.back"));
        JButton btnNext = new JButton(getBundle().getString("general.action.next"));
        JButton btnReload = new JButton(getBundle().getString("general.action.reload"));

        btnNext.addActionListener(e -> {
            int x = table.getSelectedRow();
            if (x != -1) {
                int r = table.convertRowIndexToModel(x);
                cardLayout.show(this, "resultPanel");
                analyze(model.get(r).getMountPoint());
            } else {
                JOptionPane.showMessageDialog(this, getBundle().getString("app.tools_diskspace.action.select_partition"));
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(this, "firstPanel"));
        
        btnReload.addActionListener(e -> {
            // TODO
        });
        
        Box bottomBox = Box.createHorizontalBox();
        
        bottomBox.add(chkRunAsSuperUser2);
        bottomBox.add(Box.createHorizontalStrut(10));
        bottomBox.add(btnReload);
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(btnBack);
        bottomBox.add(Box.createHorizontalStrut(10));
        bottomBox.add(btnNext);
        bottomBox.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblTitle = new JLabel(getBundle().getString("app.tools_diskspace.action.select_volume"));
        lblTitle.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(jsp);
        panel.add(bottomBox, BorderLayout.SOUTH);
        return panel;
    }
    
    @Override
    protected void createUI() {
        cardLayout.show(this, "firstPage");
    }

    private void listPartitions(SSHConnectionHandler instance, IStopper stopFlag) {
        try {
            StringBuilder output = new StringBuilder();
            
            SSHCommandRunner sshCommandRunner = new SSHCommandRunner()
                    .withCommand("df -P -k")
                    .withStdoutAppendable(output)
                    .withEnvVar("POSIXLY_CORRECT", "1")
                    .withStopper(stopFlag);
            
            instance.exec(sshCommandRunner);
            
            if (sshCommandRunner.getResult() == 0) {
                List<PartitionEntry> list = new ArrayList<>();
                boolean first = true;
                for (String line : output.toString().split("\n")) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    if (!line.trim().startsWith("/dev/")) {
                        continue;
                    }
                    String[] arr = line.split("\\s+");
                    if (arr.length < 6)
                        continue;
                    PartitionEntry ent = new PartitionEntry(arr[0], arr[5], Long.parseLong(arr[1].trim()) * 1024,
                            Long.parseLong(arr[2].trim()) * 1024, Long.parseLong(arr[3].trim()) * 1024,
                            Double.parseDouble(arr[4].replace("%", "").trim()));
                    list.add(ent);
                }
                SwingUtilities.invokeLater(() -> {
                    model.clear();
                    model.add(list);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Partition listing done");
        }
    }

    private void listVolumes() {
        IStopper.Default stopFlag = new IStopper.Default();
        holder.submitSSHOperationStoppable((guiHandle, instance) -> {
            System.out.println("Listing partitions");
            listPartitions(instance, stopFlag);
        }, stopFlag);
    }

    private void analyze(String path) {
        this.lastAnalyzedPath = path;
        System.out.println("Analyzing path: " + path);
        IStopper.Default stopFlag = new IStopper.Default();
        cardLayout.show(this, "Results");
        
        holder.submitSSHOperationStoppable((guiHandle, instance) -> {
            DiskAnalysisTask task = new DiskAnalysisTask(path, chkRunAsSuperUser1.isSelected(), stopFlag, guiHandle, instance);
            task.run();
            DiskUsageEntry res = task.getRoot();
            if(res != null){
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Result found");
                    DefaultMutableTreeNode root = new DefaultMutableTreeNode(res, true);
                    root.setAllowsChildren(true);
                    createTree(root, res);
                    treeModel.setRoot(root);
                });
            }
        }, stopFlag);
    }

    private void createTree(DefaultMutableTreeNode treeNode, DiskUsageEntry entry) {
        entry.getChildren().sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
        for (DiskUsageEntry ent : entry.getChildren()) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(ent, true);
            child.setAllowsChildren(true);
            treeNode.add(child);
            createTree(child, ent);
        }
    }
    
    @Override
    protected void onComponentVisible() {
    
    }
    
    @Override
    protected void onComponentHide() {
    
    }
}
