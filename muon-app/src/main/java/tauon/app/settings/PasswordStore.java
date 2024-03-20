package tauon.app.settings;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tauon.app.App;
import tauon.app.ui.dialogs.sessions.SavedSessionTree;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.*;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static util.Constants.configDir;

public final class PasswordStore {
    private static KeyStore keyStore;
    private static PasswordStore instance;

    private final AtomicBoolean unlocked = new AtomicBoolean(false);
    private KeyStore.PasswordProtection protParam;

    private Map<String, PasswordEntry> passwordMap = new HashMap<>();

    private PasswordStore() throws KeyStoreException {
        keyStore = KeyStore.getInstance("PKCS12");
    }

    public static synchronized PasswordStore getSharedInstance() throws Exception {
        if (instance == null) {
            instance = new PasswordStore();
        }
        return instance;
    }

    public boolean isUnlocked() {
        return unlocked.get();
    }

    public synchronized void unlockStore(char[] password) throws Exception {
        protParam = new KeyStore.PasswordProtection(password, "PBEWithHmacSHA256AndAES_256", null);
        File filePasswordStore = new File(configDir, "passwords.pfx");
        if (!filePasswordStore.exists()) {
            keyStore.load(null, protParam.getPassword());
            unlocked.set(true);
            return;
        }
        try (InputStream in = new FileInputStream(filePasswordStore)) {
            keyStore.load(in, protParam.getPassword());
            loadPasswords();
            unlocked.set(true);
        }
    }

    private synchronized void loadPasswords() throws Exception {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry("passwords", protParam);

        PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);

        char[] chars = keySpec.getPassword();

        this.passwordMap = deserializePasswordMap(chars);
    }

    private Map<String, PasswordEntry> deserializePasswordMap(char[] chars) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        try{
            return objectMapper.readValue(new CharArrayReader(chars), new TypeReference<>() {
            });
        }catch(Exception e){
            Map<String, char[]> mapOfString = objectMapper.readValue(new CharArrayReader(chars), new TypeReference<>() {
            });
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

    public synchronized PasswordEntry getSavedPassword(String alias) throws Exception {
        return this.passwordMap.get(alias);
    }
    
    public synchronized void savePassword(String alias, char[] password, char[][] hopPasswords) throws Exception {
        PasswordEntry passwordEntry = new PasswordEntry();
        passwordEntry.infoPassword = password;
        passwordEntry.hopPasswords = hopPasswords;
        this.passwordMap.put(alias, passwordEntry);
    }
    
    public synchronized void savePassword(String alias, String password, char[][] hopPasswords) throws Exception {
        PasswordEntry passwordEntry = new PasswordEntry();
        passwordEntry.infoPassword = password == null || password.isBlank() ? null : password.toCharArray();
        passwordEntry.hopPasswords = hopPasswords;
        this.passwordMap.put(alias, passwordEntry);
    }

    public synchronized void saveKeyStore() throws Exception {

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBE");
        SecretKey generatedSecret = secretKeyFactory
                .generateSecret(new PBEKeySpec(serializePasswordMap(this.passwordMap)));
        keyStore.setEntry("passwords", new SecretKeyEntry(generatedSecret), protParam);

        System.out.println("Password protection: " + protParam.getProtectionAlgorithm());

        try (OutputStream out = new FileOutputStream(new File(configDir, "passwords.pfx"))) {
            keyStore.store(out, protParam.getPassword());
        }
    }

    private boolean unlockStore() {
        if (this.isUnlocked()) {
            return true;
        }

        if (App.getGlobalSettings().isUsingMasterPassword()) {
            return unlockUsingMasterPassword();
        } else {
            try {
                unlockStore(new char[0]);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    public synchronized void populatePassword(SavedSessionTree savedSessionTree) {
        if (!unlockStore()) {
            return;
        }
        if (savedSessionTree != null) {
            populatePassword(savedSessionTree.getFolder());
        }

    }

    private void populatePassword(SessionFolder folder) {
        for (SessionInfo info : folder.getItems()) {
            try {
                PasswordEntry password = this.getSavedPassword(info.getId());
                
                if(password.infoPassword != null){
                    info.setPassword(new String(password.infoPassword));
                }
                
                int i = 0;
                for(char[] hopPass: password.hopPasswords){
                    if(hopPass != null && i < info.getJumpHosts().size()){
                        info.getJumpHosts().get(i).setPassword(new String(hopPass));
                    }
                }
                
            } catch (Exception e) {
            }
        }
        for (SessionFolder f : folder.getFolders()) {
            populatePassword(f);
        }
    }

    public void savePasswords(SavedSessionTree savedSessionTree) {
        if (!this.isUnlocked()) {
            if (App.getGlobalSettings().isUsingMasterPassword()) {
                if (!unlockUsingMasterPassword()) {
                    return;
                }
            } else {
                try {
                    unlockStore(new char[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        savePassword(savedSessionTree.getFolder());
        try {
            saveKeyStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePassword(SessionFolder folder) {
        for (SessionInfo info : folder.getItems()) {
//            String password = info.getPassword();
//            if (password != null && !password.isEmpty()) {
                try {
                    savePassword(info.getId(), info.getPassword(), info.getHopPasswords());
                } catch (Exception e) {
                    e.printStackTrace();
                }
//            }
        }
        for (SessionFolder f : folder.getFolders()) {
            savePassword(f);
        }
    }

    private boolean unlockUsingMasterPassword() {
        while (true) {
            try {
                JPasswordField txtPass = new JPasswordField(30);
                if (JOptionPane.showOptionDialog(App.getAppWindow(), new Object[]{"Master password", txtPass},
                        App.bundle.getString("master_password"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                        null) == JOptionPane.OK_OPTION) {
                    this.unlockStore(txtPass.getPassword());
                    return true;
                }
            } catch (IOException e) {
                if (e.getCause() instanceof UnrecoverableKeyException) {
                    if (JOptionPane.showConfirmDialog(App.getAppWindow(),
                            App.bundle.getString("incorrect_password")) != JOptionPane.YES_OPTION) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (JOptionPane.showConfirmDialog(App.getAppWindow(),
                    App.bundle.getString("error_loading_password")) != JOptionPane.YES_OPTION) {
                break;
            }
        }
        return false;
    }

    public boolean changeStorePassword(char[] newPassword) throws Exception {
        if (!unlockStore()) {
            return false;
        }

        Enumeration<String> aliases = keyStore.aliases();
        Map<String, PasswordEntry> passMap = new HashMap<>();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            passMap.put(alias, getSavedPassword(alias));
            keyStore.deleteEntry(alias);
        }

        protParam = new KeyStore.PasswordProtection(newPassword, "PBEWithHmacSHA256AndAES_256", null);

        for (Map.Entry<String, PasswordEntry> entry : passMap.entrySet()) {
            savePassword(entry.getKey(), entry.getValue().infoPassword, entry.getValue().hopPasswords);
        }
        saveKeyStore();
        return true;
    }
    
    public static class PasswordEntry{
        public char[] infoPassword;
        public char[][] hopPasswords;
    }
}
