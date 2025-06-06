package tauon.app.settings.importers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.SitesConfigManager;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SiteInfo;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.ui.dialogs.sessions.SavedSessionTree;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.ui.utils.TreeManager;
import tauon.app.util.misc.Constants;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static tauon.app.services.LanguageService.getBundle;


public class TauonSitesImporter {
    private static final Logger LOG = LoggerFactory.getLogger(TauonSitesImporter.class);
    
    public static SavedSessionTree getSessionsFromFile(Component parent, SitesConfigManager.PasswordPromptConsumer passwordPromptConsumer, File f) throws FileNotFoundException, OperationCancelledException, AlreadyFailedException, KeyStoreException {
        
        if (!f.getName().toLowerCase().endsWith(".zip")) {
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_not_a_zip")
            );
            throw new AlreadyFailedException();
        }
        
        // Check if the file exists and is not empty
        if (!f.exists() || f.length() == 0) {
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        byte[] buffer = new byte[8192];
        
        String sessionsFileContent = null;
        byte[] passwordsFileContent = null;
        
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(f))) {
            ZipEntry ent;
            
            while ((ent = in.getNextEntry()) != null) { // Read all entries in the ZIP file
                
                if(Constants.SESSION_DB_FILE.equals(ent.getName())) {
                    
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        sessionsFileContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                    }
                    in.closeEntry();
                    
                }else if(Constants.PASSWORDS_FILE.equals(ent.getName())) {
                    
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        passwordsFileContent = byteArrayOutputStream.toByteArray();
                    }
                    in.closeEntry();
                    
                }
                
            }
            
        } catch (IOException e) {
            LOG.error("Error processing ZIP file: {}", e.getMessage(), e);
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        if(sessionsFileContent == null){
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        SavedSessionTree tree;
        try {
            tree = SitesConfigManager.readSitesTreeFromContent(sessionsFileContent);
        } catch (JsonProcessingException e) {
            LOG.error("Error reading Sites file content: {}", e.getMessage(), e);
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        if(passwordsFileContent != null) {
            
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            
            boolean passwordsUnlocked = false;
            boolean tryAgain = false;
            boolean first = true;
            while (!passwordsUnlocked) {
                char[] password = first ? null : passwordPromptConsumer.promptMasterPassword(false, tryAgain);
                if(!first) {
                    tryAgain = true;
                }
                
                KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(first ? new char[0] : password, "PBEWithHmacSHA256AndAES_256", null);
                
                char[] chars;
                
                try (InputStream in = new ByteArrayInputStream(passwordsFileContent)) {
                    
                    keyStore.load(in, protParam.getPassword());
                    
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                    KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry("passwords", protParam);
                    
                    PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                    
                    chars = keySpec.getPassword();
                    
                    Map<String, SitesConfigManager.PasswordEntry> passwordMap = SitesConfigManager.deserializePasswordMap(chars); // If it fails, backup and start new
                    
                    SitesConfigManager.populatePasswordsInto(tree.getFolder(), passwordMap);
                    
                    passwordsUnlocked = true;
                    
                } catch (Exception e) {
                    // Password incorrect, continue
                    LOG.error("Password incorrect.", e);
                }
                
                first = false;
                
            }
            
        }
        
        return tree;
    }

    public static String sanitizeString(String line, String key) {
        return line.trim().replace(key, "").replaceAll("\"", "").replaceAll("\t", "").trim();
    }
    
    public static synchronized SavedSessionTree getSessionsTauonFile(Component parent, SitesConfigManager.PasswordPromptConsumer passwordPromptConsumer) throws OperationCancelledException, AlreadyFailedException {
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
            return TauonSitesImporter.getSessionsFromFile(parent, passwordPromptConsumer, f);
        } catch (FileNotFoundException | KeyStoreException e) {
            LOG.error("Error while importing sessions.", e);
            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_file_not_found"));
            throw new AlreadyFailedException();
        }
    }
    
    public static @NotNull DefaultMutableTreeNode createNodeTreeFor(Component parent, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode parentNode, SavedSessionTree sessions) throws OperationCancelledException {
        boolean promptName = true;
        while(promptName) {
            promptName = false;
            String string = AlertDialogUtils.promptString(parent, "Name", "Give a name to the imported root folder.", AlertDialogUtils.OnEmpty.ASK_AGAIN);
            
            if (string != null) {
                
                boolean repeated = false;
                for (int i = 0; i < parentNode.getChildCount(); i++) {
                    if (parentNode.isLeaf()) {
                        Object userObject = parentNode.getUserObject();
                        if (userObject instanceof SiteInfo) {
                            if (((SiteInfo) parentNode.getUserObject()).getName().equals(string)) {
                                repeated = true;
                                break;
                            }
                        } else if (userObject instanceof SessionFolder) {
                            if (((SiteInfo) parentNode.getUserObject()).getName().equals(string)) {
                                repeated = true;
                                break;
                            }
                        }
                    }
                }
                
                if (repeated) {
                    
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
                    
                    switch ((Constants.ConflictAction) Objects.requireNonNull(cmbOptionsExistingInfo.getSelectedItem())) {
                        case OVERWRITE:
                            sessions.getFolder().setName(string);
                            break;
                        case AUTORENAME:
                            sessions.getFolder().setName("Copy of " + string);
                            break;
                        case SKIP:
                        case CANCEL:
                            throw new OperationCancelledException();
                        case PROMPT:
                            promptName = true;
                            break;
                    }
                    
                } else {
                    sessions.getFolder().setName(string);
                }
                
            }
            
        }
        
        Map<String, String> oldToNewUuid = new HashMap<>();
        TreeManager.assignNewUuidsToAll(sessions.getFolder(), rootNode, oldToNewUuid);
        
        // TODO bookmarks, pinned logs
        
        return TreeManager.createNodeTree(sessions.getFolder());
    }
    
}
