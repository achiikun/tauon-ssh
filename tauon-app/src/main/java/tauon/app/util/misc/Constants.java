package tauon.app.util.misc;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.updater.VersionEntry;

import java.io.File;

import static tauon.app.services.LanguageService.getBundle;

public class Constants {
    
    public static final String BASE_URL = "https://github.com/achiikun";//"https://github.com/devlinx9";
    public static final String HELP_URL = "https://github.com/subhra74/snowflake/wiki"; //TODO change wiki pages
    public static final String UPDATE_URL = "https://achiikun.github.io/tauon-ssh";
    public static final String API_UPDATE_URL = "https://api.github.com/repos/achiikun/tauon-ssh/releases/latest";
    public static final String REPOSITORY_URL = BASE_URL + "/tauon-ssh";
    public static final String APPLICATION_VERSION = "3.1.0";
    public static final String APPLICATION_NAME = "Tauon SSH";

    public static final VersionEntry VERSION = new VersionEntry("v" + APPLICATION_VERSION);
    public static final String UPDATE_URL2 = UPDATE_URL + "/check-update.html?v=" + APPLICATION_VERSION;
    
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
    
    public static final String PATH_MESSAGES_FILE= "i18n/messages";
    
    public static void updateStrings() {
        TransferMode.update();
        ConflictAction.update();
        FileType.update();
    }
    
    public enum ConflictAction {

        OVERWRITE(0, "overwrite"),
        AUTORENAME(1, "autorename"),
        SKIP(2, "skip"),
        PROMPT(3, "prompt"),
        CANCEL(4, "cancel");
        
        private final int key;
        private String value;

        ConflictAction(int pKey, String pValue) {
            this.key = pKey;
            this.value = pValue;
        }

        private static void update() {
            OVERWRITE.setValue(getBundle().getString("overwrite"));
            AUTORENAME.setValue(getBundle().getString("autorename"));
            SKIP.setValue(getBundle().getString("skip"));
            PROMPT.setValue(getBundle().getString("prompt"));
            CANCEL.setValue(getBundle().getString("cancel"));
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

    public enum TransferMode {

        @JsonEnumDefaultValue
        NORMAL(0,"transfer_normally"),
        BACKGROUND(1,"transfer_background");

        private final int key;
        private String value;

        TransferMode(int pKey, String pValue) {
            this.key = pKey;
            this.value = pValue;
        }
        
        private static void update() {
            NORMAL.setValue(getBundle().getString("transfer_normally"));
            BACKGROUND.setValue(getBundle().getString("transfer_background"));
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

}
