package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.settings.NamedItem;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.dialogs.sessions.SavedSessionTree;
import tauon.app.util.misc.Constants;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.*;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static tauon.app.services.LanguageService.getBundle;

public class SessionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);
    
    private static SessionService INSTANCE = null;
    
    private SavedSessionTree savedSessionTree;
    
    private Map<String, PasswordEntry> passwordMap = new HashMap<>();
    
    private boolean passwordsUnlocked;
    private char[] masterPassword;
    
    public static SessionService getInstance() {
        if(INSTANCE == null){
            INSTANCE = new SessionService();
        }
        return INSTANCE;
    }
    
    public SavedSessionTree getSessionTree(PasswordPromptConsumer passwordPromptConsumer) throws OperationCancelledException {
        if(savedSessionTree == null){
            
            boolean success = ConfigFilesService.getInstance().loadOrBackupCancellable(
                    Constants.SESSION_DB_FILE,
                    Constants.PASSWORDS_FILE,
                    (sessionsFile, passwordsFile) -> {
                        
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        
                        SavedSessionTree tempSessionTree;
                        
                        if (sessionsFile.exists()) {
                            tempSessionTree = objectMapper.readValue(sessionsFile, new TypeReference<SavedSessionTree>() {
                            });
                        }else{
                            SessionFolder rootFolder = new SessionFolder();
                            rootFolder.setName(getBundle().getString("sessions.default_root_folder_name"));
                            tempSessionTree = new SavedSessionTree();
                            tempSessionTree.setFolder(rootFolder);
                        }
                        
                        KeyStore keyStore = KeyStore.getInstance("PKCS12");
                        
                        if (passwordsFile.exists()) {
                            
                            boolean tryAgain = false;
                            while (!passwordsUnlocked) {
                                char[] password = getOrAskForMasterPassword(passwordPromptConsumer, false, tryAgain);
                                tryAgain = true;
                                
                                KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(password, "PBEWithHmacSHA256AndAES_256", null);
                                
                                char[] chars;
                                
                                try (InputStream in = new FileInputStream(passwordsFile)) {
                                    keyStore.load(in, protParam.getPassword());
                                    
                                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                                    KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry("passwords", protParam);
                                    
                                    PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                                    
                                    chars = keySpec.getPassword();
                                    
                                    this.passwordMap = deserializePasswordMap(chars); // If it fails, backup and start new
                                    this.savedSessionTree = tempSessionTree;
                                    
                                    populatePasswordsInto(savedSessionTree.getFolder(), passwordMap);
                                    
                                    passwordsUnlocked = true;
                                    
                                } catch (Exception e) {
                                    // Password incorrect, continue
                                    LOG.error("Password incorrect.", e);
                                }
                                
                            }
                            
                        }else {
                            passwordsUnlocked = true;
                            // Create an initial file with the master password
                            
                            char[] password = getOrAskForMasterPassword(passwordPromptConsumer, true, false);
                            KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(password, "PBEWithHmacSHA256AndAES_256", null);
                            
                            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBE");
                            SecretKey mapAsSecretKey = secretKeyFactory.generateSecret(
                                    new PBEKeySpec(serializePasswordMap(passwordMap))
                            );
                            keyStore.setEntry("passwords", new KeyStore.SecretKeyEntry(mapAsSecretKey), protParam);
                            
                            try (OutputStream out = new FileOutputStream(passwordsFile)) {
                                keyStore.store(out, protParam.getPassword());
                            }
                            
                        }
                    }
            );
            
            if(!success){
                SessionFolder rootFolder = new SessionFolder();
                rootFolder.setName(getBundle().getString("sessions.default_root_folder_name"));
                savedSessionTree = new SavedSessionTree();
                savedSessionTree.setFolder(rootFolder);
                passwordMap = new HashMap<>();
            }
            
        }
        
        return savedSessionTree;
    }
    
//    public synchronized void populatePassword(PasswordPromptConsumer passwordPromptConsumer) throws OperationCancelledException {
//        SavedSessionTree seesionTree = getSessionTree(passwordPromptConsumer);
//        if (seesionTree != null) {
//            populatePassword(seesionTree.getFolder());
//        }
//    }
    
    private static void populatePasswordsInto(SessionFolder folder, Map<String, PasswordEntry> passwordMap) {
        
        for (SessionInfo info : folder.getItems()) {
            try {
                PasswordEntry password = passwordMap.get(info.getId());
                
                if(password != null) {
                    
                    if (password.infoPassword != null) {
                        info.setPassword(new String(password.infoPassword));
                    }
                    
                    int i = 0;
                    for (char[] hopPass : password.hopPasswords) {
                        if (hopPass != null && i < info.getJumpHosts().size()) {
                            info.getJumpHosts().get(i).setPassword(new String(hopPass));
                        }
                    }
                    
                }
                
            } catch (Exception e) {
                LOG.warn("Passwords for session " + info.getId() + " not found.");
            }
        }
        
        for (SessionFolder f : folder.getFolders()) {
            populatePasswordsInto(f, passwordMap);
        }
    }
    
//    public void setPasswords(SavedSessionTree savedSessionTree) {
//        if (!this.isUnlocked()) {
//            if (SettingsService.getSettings().isUsingMasterPassword()) {
//                if (!unlockUsingMasterPassword()) {
//                    return;
//                }
//            } else {
//                try {
//                    unlockStore(new char[0]);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return;
//                }
//            }
//        }
//        setPasswords(savedSessionTree.getFolder());
//        try {
//            saveKeyStore();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    
    private static void setPasswords(SessionFolder folder, Map<String, PasswordEntry> passwordMap) {
        for (SessionInfo info : folder.getItems()) {
            setPassword(info.getId(), info.getPassword(), info.getHopPasswords(), passwordMap);
        }
        for (SessionFolder f : folder.getFolders()) {
            setPasswords(f, passwordMap);
        }
    }
    
    private static void setPassword(String alias, String password, char[][] hopPasswords, Map<String, PasswordEntry> passwordMap) {
        PasswordEntry e = passwordMap.computeIfAbsent(alias, k -> new PasswordEntry());
        e.infoPassword = password == null || password.isBlank() ? null : password.toCharArray();
        e.hopPasswords = hopPasswords;
    }
    
    private static void setPassword(String alias, String password, Map<String, PasswordEntry> passwordMap) {
        PasswordEntry e = passwordMap.computeIfAbsent(alias, k -> new PasswordEntry());
        e.infoPassword = password == null || password.isBlank() ? null : password.toCharArray();
    }
    
    private char[] getOrAskForMasterPassword(PasswordPromptConsumer passwordPromptConsumer, boolean creatingPassword, boolean promptTryAgain) throws OperationCancelledException {
        if (passwordsUnlocked) {
            return masterPassword;
        }
        
        if(promptTryAgain)
            masterPassword = null;
        
        if (SettingsService.getSettings().isUsingMasterPassword()) {
            if(masterPassword == null){
                masterPassword = passwordPromptConsumer.promptMasterPassword(creatingPassword, promptTryAgain);
            }
        } else {
            masterPassword = new char[0]; // No encryption
        }
        
        return masterPassword;
        
    }
    
    private Map<String, PasswordEntry> deserializePasswordMap(char[] chars) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        try{
            
            return objectMapper.readValue(new CharArrayReader(chars), new TypeReference<>() {});
            
        }catch(Exception e){
            // Try last format
            Map<String, char[]> mapOfString = objectMapper.readValue(
                    new CharArrayReader(chars),
                    new TypeReference<>() {}
            );
            
            Map<String, PasswordEntry> map = new HashMap<>();
            for(Map.Entry<String, char[]> entry: mapOfString.entrySet()){
                PasswordEntry passwordEntry = new PasswordEntry();
                passwordEntry.infoPassword = entry.getValue();
                passwordEntry.hopPasswords = new char[0][];
                map.put(entry.getKey(), passwordEntry);
            }
            
            return map;
        }
    }
    
    private char[] serializePasswordMap(Map<String, PasswordEntry> map) throws Exception {
        CharArrayWriter writer = new CharArrayWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(writer, map);
        return writer.toCharArray();
    }
    
    public boolean replaceAndSave(SessionFolder sessionFolder, String selectedId, PasswordPromptConsumer pass) throws OperationCancelledException {
        
        // First, unlock
        getSessionTree(pass);
        
        SavedSessionTree tree = new SavedSessionTree();
        tree.setFolder(sessionFolder);
        tree.setLastSelection(selectedId);
        
        Map<String, PasswordEntry> passwordEntryMap = new HashMap<>();
        setPasswords(sessionFolder, passwordEntryMap);
        
        char[] masterPassword = this.masterPassword;
        if (masterPassword == null)
            throw new RuntimeException(); // never happens
        
        if(save(tree, passwordEntryMap, masterPassword)){
            savedSessionTree = tree;
            passwordMap = passwordEntryMap;
            return true;
        }
        
        return false;
    }
    
    public boolean save(PasswordPromptConsumer pass) throws OperationCancelledException {
        SavedSessionTree tree = getSessionTree(pass);
        
        Map<String, PasswordEntry> passwordEntryMap = new HashMap<>();
        setPasswords(tree.getFolder(), passwordEntryMap);
        
        char[] masterPassword = this.masterPassword;
        if (masterPassword == null)
            throw new RuntimeException(); // never happens
        
        if(save(tree, passwordEntryMap, masterPassword)){
            passwordMap = passwordEntryMap;
            return true;
        }
        
        return false;
        
    }
    
    private boolean save(SavedSessionTree tree, Map<String, PasswordEntry> passwordMap, char[] password) {
            
            return ConfigFilesService.getInstance().saveAndKeepOldIfFails(
                Constants.SESSION_DB_FILE,
                Constants.PASSWORDS_FILE,
                (sessionsFile, passwordsFile) -> {
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.writeValue(sessionsFile, tree);
                    
                    KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(
                            password,
                            "PBEWithHmacSHA256AndAES_256",
                            null
                    );
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(null, password);
                    
                    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBE");
                    SecretKey mapAsSecretKey = secretKeyFactory.generateSecret(
                            new PBEKeySpec(serializePasswordMap(passwordMap))
                    );
                    keyStore.setEntry("passwords", new KeyStore.SecretKeyEntry(mapAsSecretKey), protParam);
                    
                    try (OutputStream out = new FileOutputStream(passwordsFile)) {
                        keyStore.store(out, protParam.getPassword());
                    }
                    
                }
        );
    }

    public static synchronized SessionFolder convertModelFromTree(DefaultMutableTreeNode node) {
        SessionFolder folder = new SessionFolder();
        folder.setName(node.getUserObject() + "");
        folder.setId(((NamedItem) node.getUserObject()).getId());
        Enumeration<TreeNode> childrens = node.children();
        while (childrens.hasMoreElements()) {
            DefaultMutableTreeNode c = (DefaultMutableTreeNode) childrens.nextElement();
            if (c.getUserObject() instanceof SessionInfo) {
                folder.getItems().add((SessionInfo) c.getUserObject());
            } else {
                folder.getFolders().add(convertModelFromTree(c));
            }
        }
        return folder;
    }

    public static synchronized DefaultMutableTreeNode getNode(SessionFolder folder) {
        NamedItem item = new NamedItem();
        item.setName(folder.getName());
        item.setId(folder.getId());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(item);
        for (SessionInfo info : folder.getItems()) {
            DefaultMutableTreeNode c = new DefaultMutableTreeNode(info.copy());
            c.setAllowsChildren(false);
            node.add(c);
        }

        for (SessionFolder folderItem : folder.getFolders()) {
            node.add(getNode(folderItem));
        }
        return node;
    }
    
    public void setPasswordsFrom(SessionInfo session) {
        setPassword(session.getId(), session.getPassword(), session.getHopPasswords(), passwordMap);
    }
    
    public boolean changeStorePassword(char[] newPassword, PasswordPromptConsumer passwordPromptConsumer) throws OperationCancelledException {
        boolean ok = save(getSessionTree(passwordPromptConsumer), passwordMap, newPassword);
        if(ok)
            this.masterPassword = newPassword;
        return ok;
    }
    
    public interface PasswordPromptConsumer {
        char[] promptMasterPassword(boolean creatingPassword, boolean promptTryAgain) throws OperationCancelledException;
    }

//    public static synchronized void updateFavourites(String id, List<String> localFolders, List<String> remoteFolders) {
//        SavedSessionTree tree = load();
//        SessionFolder folder = tree.getFolder();
//
//        updateFavourites(folder, id, localFolders, remoteFolders);
//        save(folder, tree.getLastSelection());
//    }
//
//    private static boolean updateFavourites(SessionFolder folder, String id, List<String> localFolders,
//                                            List<String> remoteFolders) {
//        for (SessionInfo info : folder.getItems()) {
//            if (info.getId().equals(id)) {
//                if (remoteFolders != null) {
//                    System.out.println("Remote folders saving: " + remoteFolders);
//                    info.setFavouriteRemoteFolders(remoteFolders);
//                }
//                if (localFolders != null) {
//                    System.out.println("Local folders saving: " + localFolders);
//                    info.setFavouriteLocalFolders(localFolders);
//                }
//                return true;
//            }
//        }
//        for (SessionFolder childFolder : folder.getFolders()) {
//            if (updateFavourites(childFolder, id, localFolders, remoteFolders)) {
//                return true;
//            }
//        }
//        return false;
//    }

    public static class PasswordEntry{
        public char[] infoPassword;
        public char[][] hopPasswords;
    }
}
