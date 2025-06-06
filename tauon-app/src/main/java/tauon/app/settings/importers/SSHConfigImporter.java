package tauon.app.settings.importers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.SitesConfigManager;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SiteInfo;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.ui.dialogs.sessions.PasswordPromptHelper;
import tauon.app.ui.dialogs.sessions.SavedSessionTree;
import tauon.app.ui.dialogs.sessions.SessionsExportImport;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;


public class SSHConfigImporter {
    private static final Logger LOG = LoggerFactory.getLogger(SSHConfigImporter.class);


    static final String HOST_TEXT = "Host";
    static final String IP_TEXT = "HostName";
    static final String PORT_TEXT = "Port";
    static final String IDENTITY_FILE_TEXT = "IdentityFile";
    static final String USER_TEXT = "User";

    public static List<SiteInfo> getSessionsFromFile(File file) throws FileNotFoundException {
        List<SiteInfo> siteInfoList = new ArrayList<>();
        Scanner myReader = new Scanner(file);
        String linea = myReader.hasNextLine() ? myReader.nextLine() : null;
        SiteInfo info = new SiteInfo();
        if (linea.contains(HOST_TEXT)) {
            info.setName(sanitizeString(linea, HOST_TEXT));
        }
        while (myReader.hasNextLine()) {
            linea = myReader.nextLine();
            if (linea.contains(IP_TEXT)) {
                info.setHost(sanitizeString(linea, IP_TEXT));
            } else if (linea.contains(USER_TEXT)) {
                info.setUser(sanitizeString(linea, USER_TEXT));
            } else if (linea.contains(PORT_TEXT)) {
                info.setPort(Integer.parseInt(sanitizeString(linea, PORT_TEXT)));
            } else if (linea.contains(IDENTITY_FILE_TEXT)) {
                info.setPrivateKeyFile(sanitizeString(linea, IDENTITY_FILE_TEXT));
            } else if (linea.contains(HOST_TEXT)) {
                if (info.getName() != null) {
                    siteInfoList.add(info);
                }
                info = new SiteInfo();
                info.setName(sanitizeString(linea, HOST_TEXT));
            }
        }
        if (info.getName() != null) {
            siteInfoList.add(info);
        }

        return siteInfoList;
    }

    public static String sanitizeString(String line, String key) {
        return line.trim().replace(key, "").replaceAll("\"", "").replaceAll("\t", "").trim();
    }
    
    public static synchronized List<SiteInfo> getSessionsSSHConfigFile(Component parent) throws OperationCancelledException, AlreadyFailedException {
        NativeFileChooser jfc = new NativeFileChooser();
        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            throw new OperationCancelledException();
        }
        File f = jfc.getSelectedFile();
        
        if (f == null || !f.canRead()) {
            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_file_not_found"));
            throw new AlreadyFailedException();
        }
        
        try {
            return SSHConfigImporter.getSessionsFromFile(f);
        } catch (FileNotFoundException e) {
            LOG.error("Error while importing sessions.", e);
            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_file_not_found"));
            throw new AlreadyFailedException();
        }
    }
    
    public static void importSessions(Component parent, DefaultMutableTreeNode parentNode, List<SiteInfo> sessions, int[] indexes) throws OperationCancelledException {
        
        // Check repetitions
        
        int repeated = 0;
        for (int index: indexes){
            SiteInfo siteInfo = sessions.get(index);
            
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                if(parentNode.isLeaf()){
                    Object userObject = parentNode.getUserObject();
                    if(userObject instanceof SiteInfo){
                        if(((SiteInfo)parentNode.getUserObject()).getName().equals(siteInfo.getName()))
                            repeated++;
                    }
                }
            }
        }
        
        Constants.ConflictAction cc = null;
        if(repeated > 0) {
            
            DefaultComboBoxModel<Constants.ConflictAction> conflictOptionsCmb = new DefaultComboBoxModel<>(Constants.ConflictAction.values());
            conflictOptionsCmb.removeAllElements();
            for (Constants.ConflictAction conflictActionCmb : Constants.ConflictAction.values()) {
                if (conflictActionCmb.getKey() < 3) {
                    conflictOptionsCmb.addElement(conflictActionCmb);
                }
            }
            
            JComboBox<Constants.ConflictAction> cmbOptionsExistingInfo = new JComboBox<>(conflictOptionsCmb);
            
            if (JOptionPane.showOptionDialog(
                    parent,
                    new Object[]{
                            getBundle().getString("app.sites.import_dialog.action_for_repeated_sessions"),
                            cmbOptionsExistingInfo
                    },
                    getBundle().getString("app.sites.import_dialog.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null
            ) != JOptionPane.OK_OPTION) {
                throw new OperationCancelledException();
            }
            
            cc = (Constants.ConflictAction) cmbOptionsExistingInfo.getSelectedItem();
        }
        
        int total = 0;
        int imported = 0;
        int skipped = 0;
        int overwritten = 0;
        int renamed = 0;
        
        for (int index: indexes){
            SiteInfo siteInfo = sessions.get(index);
            
            DefaultMutableTreeNode isRepeatedIndex = null;
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                if(parentNode.isLeaf()){
                    Object userObject = parentNode.getUserObject();
                    if(userObject instanceof SiteInfo){
                        if(((SiteInfo)parentNode.getUserObject()).getName().equals(siteInfo.getName())){
                            isRepeatedIndex = childNode;
                            break;
                        }
                    }
                }
            }
            
            if(isRepeatedIndex != null){
                if (cc == Constants.ConflictAction.SKIP) {
                    skipped++;
                    continue;
                }
                if (cc == Constants.ConflictAction.AUTORENAME) {
                    siteInfo.setName("Copy of " + siteInfo.getName());
                    renamed++;
                } else if (cc == Constants.ConflictAction.OVERWRITE) {
                    isRepeatedIndex.setUserObject(siteInfo);
                    overwritten++;
                }
            }else{
                DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(siteInfo);
                node1.setAllowsChildren(false);
                parentNode.add(node1);
                imported++;
            }
            
            total++;
        }
        
//        SavedSessionTree tree = SitesConfigManager.getInstance().getSessionTree(new PasswordPromptHelper(parent));
//        SessionFolder folder = tree.getFolder();
//
//        List<SessionFolder> folders = folder.getFolders();
//        int total = sessions.size();
//        SessionFolder sessionFolder;
//        for (SiteInfo session : sessions) {
//            session.setId(UUID.randomUUID().toString());
//
//            sessionFolder = new SessionFolder();
//            sessionFolder.setId(UUID.randomUUID().toString());
//            sessionFolder.setName(session.getName());
//
//            List<SiteInfo> item = new ArrayList<>();
//            item.add(session);
//            sessionFolder.setItems(item);
//
//            if (folders.contains(sessionFolder)) {
//                if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.SKIP) {
//                    continue;
//                }
//                if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.AUTORENAME) {
//                    sessionFolder.setName("Copy of " + sessionFolder.getName());
//                    folders.add(sessionFolder);
//                } else if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.OVERWRITE) {
//                    folders.set(folders.indexOf(sessionFolder), sessionFolder);
//                }
//                imported++;
//                continue;
//            }
//
//            SitesConfigManager.getInstance().setPasswordsFrom(session);
//            folders.add(sessionFolder);
//            imported++;
//        }
//
//        folder.setFolders(folders);
//
//        SitesConfigManager.getInstance().save(new PasswordPromptHelper(parent));
        
        JOptionPane.showMessageDialog(parent,
                FormatUtils.$$(
                        getBundle().getString("app.sites.import_dialog.result.content"),
                        Map.of(
                                "total", total,
                                "imported", imported,
                                "skipped", skipped,
                                "renamed", renamed,
                                "overwritten", overwritten
                        )
                ),
                getBundle().getString("app.sites.import_dialog.result.title"),
                JOptionPane.INFORMATION_MESSAGE
        );
        
    }

}
