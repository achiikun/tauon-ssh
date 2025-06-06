package tauon.app.util.misc;

import tauon.app.ssh.filesystem.FileType;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import static tauon.app.services.LanguageService.getBundle;

public class Constants {
    
    public static final String BASE_URL = "https://github.com/achiikun";//"https://github.com/devlinx9";
    public static final String HELP_URL = "https://github.com/subhra74/snowflake/wiki"; //TODO change wiki pages
//    public static final String UPDATE_URL = "https://achiikun.github.io/tauon-ssh";
    public static final String API_UPDATE_URL = "https://api.github.com/repos/achiikun/tauon-ssh/releases/latest";
    public static final String REPOSITORY_URL = BASE_URL + "/tauon-ssh";
    public static final String REPOSITORY_TAG_URL = BASE_URL + "/tauon-ssh/releases/tag/";
//    public static final String APPLICATION_VERSION = "3.1.0";
    public static final String APPLICATION_NAME = "Tauon SSH";

//    public static final String UPDATE_URL2 = UPDATE_URL + "/check-update.html?v=" + APPLICATION_VERSION;
    
    public static File[] OLD_CONFIG_DIRS = {
            new File(System.getProperty("user.home") + File.separatorChar + ".muon-ssh"),
            new File(System.getProperty("user.home") + File.separatorChar + "muon-ssh"),
    };
    public static File CONFIG_DIR = new File(System.getProperty("user.home"), ".tauon-ssh");
    
    public static final String SESSION_DB_FILE = "session-store.json";
    public static final String SETTINGS_DB_FILE = "settings.json";
    public static final String SNIPPETS_FILE = "snippets.json";
    public static final String PINNED_LOGS = "pinned-logs.json";
    public static final String TRANSFER_HOSTS = "transfer-hosts.json";
    public static final String BOOKMARKS_FILE = "bookmarks.json";
    public static final String PASSWORDS_FILE = "passwords.pfx";
    public static final String KNOWN_HOSTS_FILE = "known_hosts";
    
    public static final Set<String> ALL_CONFIG_FILES = Set.of(
            SESSION_DB_FILE, SETTINGS_DB_FILE, SNIPPETS_FILE, PINNED_LOGS, TRANSFER_HOSTS, BOOKMARKS_FILE, PASSWORDS_FILE, KNOWN_HOSTS_FILE
    );

    public static final Set<String> SESSIONS_FILES = Set.of(
            SESSION_DB_FILE, PINNED_LOGS, BOOKMARKS_FILE, PASSWORDS_FILE, KNOWN_HOSTS_FILE
    );
    
    public static final String PATH_MESSAGES_FILE= "i18n/messages";
    
    public static final VersionEntry VERSION;
    
    static {
        Properties p = new Properties();
        try {
            p.load(Constants.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        VERSION = new VersionEntry("v" + p.getProperty("tauon-version"));
    }
    
    public static void updateStrings() {
//        TransferMode.update();
        ConflictAction.update();
        FileType.update();
    }
    
    public enum ConflictAction {

        OVERWRITE(0, "app.files.action.overwrite"),
        AUTORENAME(1, "app.files.action.autorename"),
        SKIP(2, "app.files.action.skip"),
        PROMPT(3, "app.files.action.prompt"),
        CANCEL(4, "general.action.cancel");
        
        private final int key;
        private String value;

        ConflictAction(int pKey, String pValue) {
            this.key = pKey;
            this.value = pValue;
        }

        private static void update() {
            OVERWRITE.setValue(getBundle().getString("app.files.action.overwrite"));
            AUTORENAME.setValue(getBundle().getString("app.files.action.autorename"));
            SKIP.setValue(getBundle().getString("app.files.action.skip"));
            PROMPT.setValue(getBundle().getString("app.files.action.prompt"));
            CANCEL.setValue(getBundle().getString("general.action.cancel"));
        }

        public int getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String pValue) {
            this.value = pValue;
        }

        @Override
        public String toString() {
            return value;
        }
    }

//    public enum TransferMode {
//
//        @JsonEnumDefaultValue
//        NORMAL(0, "app.files.action.transfer_normally"),
//        BACKGROUND(1, "app.files.action.transfer_background");
//
//        private final int key;
//        private String value;
//
//        TransferMode(int pKey, String pValue) {
//            this.key = pKey;
//            this.value = pValue;
//        }
//
//        private static void update() {
//            NORMAL.setValue(getBundle().getString("app.files.action.transfer_normally"));
//            BACKGROUND.setValue(getBundle().getString("app.files.action.transfer_background"));
//        }
//
//        public int getKey() {
//            return key;
//        }
//
//        public String getValue() {
//            return value;
//        }
//
//        public void setValue(String pValue) {
//            this.value = pValue;
//        }
//
//        @Override
//        public String toString() {
//            return value;
//        }
//    }

}
