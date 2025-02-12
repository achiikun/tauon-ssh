package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import tauon.app.settings.Settings;
import tauon.app.util.misc.Constants;

public class SettingsConfigManager {
    
    private static SettingsConfigManager INSTANCE = null;
    
    private Settings settings;
    
    public static SettingsConfigManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new SettingsConfigManager();
        }
        return INSTANCE;
    }
    
    @NotNull
    public static Settings getSettings() {
        return getInstance().settings;
    }
    
    public boolean setAndSave(SettingsConsumer settingsConsumer) {
        try{
            settingsConsumer.consume(settings);
            return save();
        }catch(Exception e){
            return false;
        }
        
    }
    
    private SettingsConfigManager(){
    
    }
    
    public void initialize() {
        
        boolean loaded = ConfigFilesService.getInstance().loadOrBackup(Constants.SETTINGS_DB_FILE, file -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            if (file.exists()) {
                settings = objectMapper.readValue(file, new TypeReference<>() {});
                settings.fillUncompletedMaps();
            }else{
                settings = new Settings();
            }
        });
        
        if(!loaded)
            settings = new Settings();
        
        // Set language to enums
        Constants.updateStrings();
        
    }
    
//    public static synchronized void loadSettings() {
//        File file = new File(CONFIG_DIR, CONFIG_DB_FILE);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        if (file.exists()) {
//            try {
//                settings = objectMapper.readValue(file, new TypeReference<>() {
//                });
//                settings.fillUncompletedMaps();
//                return;
//            } catch (IOException e) {
//                LOG.error(e.getMessage(), e);
//            }
//        }
//        settings = new Settings();
//    }

//    public static synchronized Settings loadSettings2() {
//        File file = new File(CONFIG_DIR, CONFIG_DB_FILE);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        if (file.exists()) {
//            try {
//                settings = objectMapper.readValue(file, new TypeReference<>() {
//                });
//                settings.fillUncompletedMaps();
//                return settings;
//            } catch (IOException e) {
//                LOG.error(e.getMessage(), e);
//            }
//        }
//        settings = new Settings();
//        return settings;
//    }
    
    private boolean save() {
        return ConfigFilesService.getInstance().saveAndKeepOldIfFails(Constants.SETTINGS_DB_FILE, file -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, settings);
        });
    }
    
    
    public interface SettingsConsumer {
        void consume(Settings settings) throws Exception;
    }
}
