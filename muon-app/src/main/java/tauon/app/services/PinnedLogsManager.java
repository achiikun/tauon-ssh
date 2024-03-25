package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static tauon.app.util.misc.Constants.PINNED_LOGS;

public final class PinnedLogsManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(PinnedLogsManager.class);
    
    private static PinnedLogsManager INSTANCE = null;
    
    private boolean loaded = false;
    private Map<String, List<String>> pinnedLogsMap = new HashMap<>();
    
    public static PinnedLogsManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new PinnedLogsManager();
        }
        return INSTANCE;
    }
    
    private PinnedLogsManager() {

    }
    
    private boolean load(){
        if(!loaded){
            loaded = true;
            
            boolean success = ConfigFilesService.getInstance().loadOrBackup(PINNED_LOGS, file -> {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if (file.exists()) {
                    pinnedLogsMap = objectMapper.readValue(file, new TypeReference<>() {});
                }
            });
            
            if(!success)
                LOG.error("Error loading snippets.");
            
        }
        return true;
    }

    public Map<String, List<String>> getAll() {
        load();
        return Collections.synchronizedMap(pinnedLogsMap);
    }

    public boolean save() {
        return ConfigFilesService.getInstance().saveAndKeepOldIfFails(PINNED_LOGS, file -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, pinnedLogsMap);
        });
    }

    public synchronized void addEntry(String id, String path) {
        if (id == null) {
            id = "";
        }
        load();
        List<String> bookmarks = pinnedLogsMap.get(id);
        if (bookmarks == null) {
            bookmarks = new ArrayList<>();
        }
        bookmarks.add(path);
        pinnedLogsMap.put(id, bookmarks);
        save();
    }

    public void addEntry(String id, List<String> path) {
        if (id == null) {
            id = "";
        }
        load();
        List<String> pinnedLogs = pinnedLogsMap.get(id);
        if (pinnedLogs == null) {
            pinnedLogs = new ArrayList<>();
        }
        pinnedLogs.addAll(path);
        pinnedLogsMap.put(id, pinnedLogs);
        save();
    }

    public List<String> getPinnedLogs(String id) {
        if (id == null) {
            id = "";
        }
        load();
        
        List<String> bookmarks = pinnedLogsMap.get(id);
        if (bookmarks != null) {
            return new ArrayList<>(bookmarks);
        }
        return new ArrayList<>();
    }
}
