package tauon.app.ui.dialogs.sessions;

import tauon.app.App;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.services.ConfigFilesService;
import tauon.app.settings.NamedItem;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SessionInfo;
import tauon.app.services.SessionService;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.ui.components.misc.AutoScrollingJTree;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.ui.components.misc.SkinnedSplitPane;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import static tauon.app.services.LanguageService.getBundle;

public class NewSessionDlg extends JDialog implements ActionListener, TreeSelectionListener, TreeModelListener {

    private static final long serialVersionUID = -1182844921331289546L;
    
    private final Window window;
    
    private DefaultTreeModel treeModel;
    private JTree tree;
    private DefaultMutableTreeNode rootNode;
    private JScrollPane jsp;
    private SessionInfoPanel sessionInfoPanel;
    private JButton btnNewHost, btnDel, btnDup, btnNewFolder, btnExport, btnImport;
    private JButton btnConnect, btnCancel, btnSaveAndClose;
    private JTextField txtName;
    private JPanel namePanel;
    private NamedItem selectedInfo;
    private String lastSelected;
    private JPanel prgPanel;
    private JPanel pdet;
    private SessionInfo info;
    private JLabel lblName;
    
    private boolean buttonTriggeredClosing;
    
    public NewSessionDlg(Window wnd) throws OperationCancelledException {
        super(wnd);
        this.window = wnd;
        createUI();
    }

    private void createUI() throws OperationCancelledException {
        setBackground(new Color(245, 245, 245));
        setLayout(new BorderLayout());

        setSize(800, 600);
        setModal(true);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(buttonTriggeredClosing){
                    buttonTriggeredClosing = false;
                }else {
                    try {
                        save();
                        dispose();
                    } catch (OperationCancelledException | AlreadyFailedException ignore) {
                    
                    }
                }
            }
        });

        setTitle(getBundle().getString("session_manager"));

        treeModel = new DefaultTreeModel(null, true);
        treeModel.addTreeModelListener(this);
        tree = new AutoScrollingJTree(treeModel);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getSelectionModel().addTreeSelectionListener(this);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (node == null || node.getAllowsChildren()) return;
                    connectClicked();
                }
            }
        });


        tree.setEditable(false);
        jsp = new JScrollPane(tree);
        jsp.setBorder(new LineBorder(App.skin.getDefaultBorderColor(), 1));

        btnNewHost = new JButton(getBundle().getString("new_site"));
        btnNewHost.addActionListener(this);
        btnNewHost.putClientProperty("button.name", "btnNewHost");
        btnNewFolder = new JButton(getBundle().getString("new_folder"));
        btnNewFolder.addActionListener(this);
        btnNewFolder.putClientProperty("button.name", "btnNewFolder");
        btnDel = new JButton(getBundle().getString("remove"));
        btnDel.addActionListener(this);
        btnDel.putClientProperty("button.name", "btnDel");
        btnDup = new JButton(getBundle().getString("duplicate"));
        btnDup.addActionListener(this);
        btnDup.putClientProperty("button.name", "btnDup");

        btnConnect = new JButton(getBundle().getString("connect"));
        btnConnect.addActionListener(this);
        btnConnect.putClientProperty("button.name", "btnSaveAndConnect");

        btnCancel = new JButton(getBundle().getString("cancel"));
        btnCancel.addActionListener(this);
        btnCancel.putClientProperty("button.name", "btnCancelWithoutSaving");
        
        btnSaveAndClose = new JButton(getBundle().getString("save"));
        btnSaveAndClose.addActionListener(this);
        btnSaveAndClose.putClientProperty("button.name", "btnSaveAndClose");

        btnExport = new JButton(getBundle().getString("export"));
        btnExport.addActionListener(this);
        btnExport.putClientProperty("button.name", "btnExport");

        btnImport = new JButton(getBundle().getString("import"));
        btnImport.addActionListener(this);
        btnImport.putClientProperty("button.name", "btnImport");

        normalizeButtonSize();

        Box box1 = Box.createHorizontalBox();
        box1.setBorder(new EmptyBorder(10, 10, 10, 10));
        box1.add(Box.createHorizontalGlue());
        box1.add(Box.createHorizontalStrut(10));
        box1.add(btnConnect);
        box1.add(Box.createHorizontalStrut(10));
        box1.add(btnSaveAndClose);
        box1.add(Box.createHorizontalStrut(10));
        box1.add(btnCancel);

        GridLayout gl = new GridLayout(3, 2, 5, 5);
        JPanel btnPane = new JPanel(gl);
        btnPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        btnPane.add(btnNewHost);
        btnPane.add(btnNewFolder);
        btnPane.add(btnDup);
        btnPane.add(btnDel);
        btnPane.add(btnExport);
        btnPane.add(btnImport);

        JSplitPane splitPane = new SkinnedSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel treePane = new JPanel(new BorderLayout());
        treePane.setBorder(new EmptyBorder(10, 10, 10, 0));
        treePane.add(jsp);
        treePane.add(btnPane, BorderLayout.SOUTH);

        add(treePane, BorderLayout.WEST);

        sessionInfoPanel = new SessionInfoPanel();

        namePanel = new JPanel();

        JPanel pp = new JPanel(new BorderLayout());
        pp.add(namePanel, BorderLayout.NORTH);
        pp.add(sessionInfoPanel);

        pdet = new JPanel(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(pp);
        scrollPane.setBorder(null);
        pdet.add(scrollPane);
        pdet.add(box1, BorderLayout.SOUTH);


        BoxLayout boxLayout = new BoxLayout(namePanel, BoxLayout.PAGE_AXIS);
        namePanel.setLayout(boxLayout);

        namePanel.setBorder(new EmptyBorder(10, 0, 0, 10));

        lblName = new JLabel(getBundle().getString("name"));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblName.setHorizontalAlignment(JLabel.LEADING);
        lblName.setBorder(new EmptyBorder(0, 0, 5, 0));


        txtName = new SkinnedTextField(10);
        txtName.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtName.setHorizontalAlignment(JLabel.LEADING);
        txtName.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent arg0) {
                updateName();
            }

            @Override
            public void insertUpdate(DocumentEvent arg0) {
                updateName();
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                updateName();
            }

            private void updateName() {
                selectedInfo.setName(txtName.getText());
                TreePath parentPath = tree.getSelectionPath();
                DefaultMutableTreeNode parentNode = null;

                if (parentPath != null) {
                    parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
                    if (parentNode != null) {
                        treeModel.nodeChanged(parentNode);
                    }
                }
            }
        });

        namePanel.add(lblName);
        namePanel.add(txtName);

        prgPanel = new JPanel();

        JLabel lbl = new JLabel(getBundle().getString("connecting"));
        prgPanel.add(lbl);

        splitPane.setLeftComponent(treePane);
        splitPane.setRightComponent(pdet);

        add(splitPane);

        lblName.setVisible(false);
        txtName.setVisible(false);
        sessionInfoPanel.setVisible(false);
        btnConnect.setVisible(false);

        loadTree(SessionService.getInstance().getSessionTree(new PasswordPromptHelper(window)));
    }

    private void loadTree(SavedSessionTree stree) {
        this.lastSelected = stree.getLastSelection();
        rootNode = SessionService.getNode(stree.getFolder());
        rootNode.setAllowsChildren(true);
        treeModel.setRoot(rootNode);
        try {
            if (this.lastSelected != null) {
                selectNode(lastSelected, rootNode);
            } else {
                DefaultMutableTreeNode n = null;
                n = findFirstInfoNode(rootNode);
                if (n == null) {
                    SessionInfo sessionInfo = new SessionInfo();
                    sessionInfo.setName(getBundle().getString("new_site"));
                    sessionInfo.setId(UUID.randomUUID().toString());
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(sessionInfo);
                    childNode.setUserObject(sessionInfo);
                    childNode.setAllowsChildren(false);
                    treeModel.insertNodeInto(childNode, rootNode, rootNode.getChildCount());
                    n = childNode;
                    tree.scrollPathToVisible(new TreePath(n.getPath()));
                    TreePath path = new TreePath(n.getPath());
                    tree.setSelectionPath(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        treeModel.nodeChanged(rootNode);
    }

    private boolean selectNode(String id, DefaultMutableTreeNode node) {
        if (id.equals((((NamedItem) node.getUserObject()).getId()))) {
            TreePath path = new TreePath(node.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (selectNode(id, child)) {
                return true;
            }
        }
        return false;
    }

    private DefaultMutableTreeNode findFirstInfoNode(DefaultMutableTreeNode node) {
        if (!node.getAllowsChildren()) {
            return node;
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = findFirstInfoNode((DefaultMutableTreeNode) node.getChildAt(i));
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton btn = (JButton) e.getSource();
        TreePath parentPath = tree.getSelectionPath();
        DefaultMutableTreeNode parentNode = null;

        if (parentPath != null) {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }

        switch ((String) btn.getClientProperty("button.name")) {
            case "btnNewHost":
                if (parentNode == null) {
                    parentNode = rootNode;
                }
                Object obj = parentNode.getUserObject();
                if (obj instanceof SessionInfo) {
                    parentNode = (DefaultMutableTreeNode) parentNode.getParent();
                    obj = parentNode.getUserObject();
                }
                SessionInfo sessionInfo = new SessionInfo();
                sessionInfo.setName(getBundle().getString("new_site"));
                sessionInfo.setId(UUID.randomUUID().toString());
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(sessionInfo);
                childNode.setUserObject(sessionInfo);
                childNode.setAllowsChildren(false);
                treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());
                tree.scrollPathToVisible(new TreePath(childNode.getPath()));
                TreePath path = new TreePath(childNode.getPath());
                tree.setSelectionPath(path);
                break;
            case "btnNewFolder":
                if (parentNode == null) {
                    parentNode = rootNode;
                }
                Object objFolder = parentNode.getUserObject();
                if (objFolder instanceof SessionInfo) {
                    parentNode = (DefaultMutableTreeNode) parentNode.getParent();
                    objFolder = parentNode.getUserObject();
                }
                SessionFolder folder = new SessionFolder();
                folder.setName(getBundle().getString("new_folder"));
                DefaultMutableTreeNode childNode1 = new DefaultMutableTreeNode(folder);
                treeModel.insertNodeInto(childNode1, parentNode, parentNode.getChildCount());
                tree.scrollPathToVisible(new TreePath(childNode1.getPath()));
                TreePath path2 = new TreePath(childNode1.getPath());
                tree.setSelectionPath(path2);
                break;
            case "btnDel":
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null && node.getParent() != null) {
                    DefaultMutableTreeNode sibling = node.getNextSibling();
                    if (sibling != null) {
                        String id = ((NamedItem) sibling.getUserObject()).getId();
                        selectNode(id, sibling);
                    } else {
                        DefaultMutableTreeNode parentNode1 = (DefaultMutableTreeNode) node.getParent();
                        tree.setSelectionPath(new TreePath(parentNode1.getPath()));
                    }
                    treeModel.removeNodeFromParent(node);
                }
                break;
            case "btnDup":
                DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node1 != null && node1.getParent() != null && (node1.getUserObject() instanceof SessionInfo)) {
                    SessionInfo info = ((SessionInfo) node1.getUserObject()).copy();
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(info);
                    child.setAllowsChildren(false);
                    treeModel.insertNodeInto(child, (MutableTreeNode) node1.getParent(), node1.getParent().getChildCount());
                    selectNode(info.getId(), child);
                } else if (node1 != null && node1.getParent() != null && (node1.getUserObject() instanceof NamedItem)) {
                    SessionFolder newFolder = new SessionFolder();
                    newFolder.setId(UUID.randomUUID().toString());
                    newFolder.setName("Copy of " + ((NamedItem) node1.getUserObject()).getName());
                    Enumeration childrens = node1.children();
                    DefaultMutableTreeNode newFolderTree = new DefaultMutableTreeNode(newFolder);
                    while (childrens.hasMoreElements()) {
                        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode) childrens.nextElement();
                        if (defaultMutableTreeNode.getUserObject() instanceof SessionInfo) {
                            SessionInfo newCopyInfo = ((SessionInfo) defaultMutableTreeNode.getUserObject()).copy();
                            newCopyInfo.setName("Copy of " + newCopyInfo.getName());
                            DefaultMutableTreeNode subChild = new DefaultMutableTreeNode(newCopyInfo);
                            subChild.setAllowsChildren(false);
                            newFolderTree.add(subChild);
                        }
                    }
                    MutableTreeNode parent = (MutableTreeNode) node1.getParent();
                    treeModel.insertNodeInto(newFolderTree, parent, node1.getParent().getChildCount());
                    selectNode(newFolder.getId(), newFolderTree);
                }
                break;
            case "btnSaveAndConnect":
                connectClicked();
                break;
            case "btnCancelWithoutSaving":
                buttonTriggeredClosing = true;
                dispose();
                break;
            case "btnSaveAndClose":
                try{
                    buttonTriggeredClosing = true;
                    save();
                    dispose();
                } catch (OperationCancelledException | AlreadyFailedException ignore) {
                
                }
                break;
            case "btnImport":
                if (parentNode == null) {
                    parentNode = rootNode;
                }
                if (parentNode.getUserObject() instanceof SessionInfo) {
                    parentNode = (DefaultMutableTreeNode) parentNode.getParent();
                }
                JComboBox<String> cmbImports = new JComboBox<>(
                        new String[]{"Putty", "WinSCP", "Muon session store", "SSH config file"});

                if (JOptionPane.showOptionDialog(this,
                        new Object[]{
                                getBundle().getString("import_from"),
                                cmbImports
                        },
                        getBundle().getString("sessions.import.dialog.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                ) == JOptionPane.OK_OPTION) {
                    
                    try{
                        
                        if (cmbImports.getSelectedIndex() < 2) {
                            // Putty, winscp
                            new ImportDlg(this, cmbImports.getSelectedIndex(), parentNode)
                                    .setVisible(true);
                            treeModel.nodeStructureChanged(parentNode);
                            
                        } else if (cmbImports.getSelectedIndex() == 3) {
                            SessionExportImport.importSessionsSSHConfig(this);
                            // Reload
                            loadTree(SessionService.getInstance().getSessionTree(new PasswordPromptHelper(this)));
                        } else {
                            SessionExportImport.importSessions(this);
                            // Reload
                            loadTree(SessionService.getInstance().getSessionTree(new PasswordPromptHelper(this)));
                        }
                        
                    }catch (OperationCancelledException | AlreadyFailedException ignored){
                    
                    }
                    
                }

                break;
            case "btnExport":
                
                NativeFileChooser jfc = new NativeFileChooser();
                if (jfc.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                    File file = jfc.getSelectedFile();
                    try {
                        ConfigFilesService.getInstance().exportTo(file);
                        AlertDialogUtils.showSuccess(
                                this,
                                FormatUtils.$$(
                                        getBundle().getString("sessions.export.done"),
                                        Map.of("file", file.getName())
                                )
                        );
                    } catch (IOException ex) {
                        AlertDialogUtils.showError(
                                this,
                                FormatUtils.$$(
                                        getBundle().getString("sessions.export.error"),
                                        Map.of("file", file.getName())
                                )
                        );
                    }
                }
                
                break;
            default:
                break;
        }
    }

    private void connectClicked() {
        try {
            
            save();
            
            this.info = (SessionInfo) selectedInfo;
            if (this.info.getHost() == null || this.info.getHost().isEmpty()) {
                JOptionPane.showMessageDialog(this, getBundle().getString("no_hostname"));
                this.info = null;
            } else {
                buttonTriggeredClosing = true;
                dispose();
            }
            
        } catch (OperationCancelledException | AlreadyFailedException ignore) {
        
        }
    }

    public SessionInfo newSession() {
        setLocationRelativeTo(window);
        setVisible(true);
        return this.info;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        System.out.println("value changed");
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (node == null)
            // Nothing is selected.
            return;

        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof SessionInfo) {
            sessionInfoPanel.setVisible(true);
            SessionInfo info = (SessionInfo) nodeInfo;
            sessionInfoPanel.setSessionInfo(info);
            selectedInfo = info;
            txtName.setVisible(true);
            lblName.setVisible(true);
            txtName.setText(selectedInfo.getName());
            btnConnect.setVisible(true);
        } else if (nodeInfo instanceof NamedItem) {
            selectedInfo = (NamedItem) nodeInfo;
            lblName.setVisible(true);
            txtName.setVisible(true);
            txtName.setText(selectedInfo.getName());
            sessionInfoPanel.setVisible(false);
            btnConnect.setVisible(false);
        }

        revalidate();
        repaint();
    }

    private void save() throws OperationCancelledException, AlreadyFailedException {
        String selectedId = null;
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            NamedItem item = (NamedItem) node.getUserObject();
            selectedId = item.getId();
        }
        if(!SessionService.getInstance().replaceAndSave(
                SessionService.convertModelFromTree(rootNode),
                selectedId,
                new PasswordPromptHelper(this)
        )){
            AlertDialogUtils.showError(this, getBundle().getString("sessions.save_failed"));
            throw new AlreadyFailedException();
        }
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        System.out.println("treeNodesChanged");
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {

    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {

    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {

    }

    private void normalizeButtonSize() {
        int width = Math.max(Math.max(btnConnect.getPreferredSize().width, btnSaveAndClose.getPreferredSize().width), btnCancel.getPreferredSize().width);
        btnConnect.setPreferredSize(new Dimension(width, btnConnect.getPreferredSize().height));
        btnSaveAndClose.setPreferredSize(new Dimension(width, btnSaveAndClose.getPreferredSize().height));
        btnCancel.setPreferredSize(new Dimension(width, btnCancel.getPreferredSize().height));
    }
}
