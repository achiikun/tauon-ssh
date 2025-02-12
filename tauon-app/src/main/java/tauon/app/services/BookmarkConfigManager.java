package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static tauon.app.util.misc.Constants.*;

public final class BookmarkConfigManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(BookmarkConfigManager.class);
    
    private static BookmarkConfigManager INSTANCE = null;
    
    private boolean loaded = false;
    private Map<String, List<String>> bookmarkMap = new HashMap<>();
    
    public static BookmarkConfigManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new BookmarkConfigManager();
        }
        return INSTANCE;
    }
    
    private BookmarkConfigManager() {

    }
    
    private boolean load(){
        if(!loaded){
            loaded = true;
            
            boolean success = ConfigFilesService.getInstance().loadOrBackup(BOOKMARKS_FILE, file -> {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if (file.exists()) {
                    bookmarkMap = objectMapper.readValue(file, new TypeReference<>() {});
                }
            });
            
            if(!success)
                LOG.error("Error loading snippets.");
            
        }
        return loaded;
    }

    public Map<String, List<String>> getAll() {
        load();
        return Collections.synchronizedMap(bookmarkMap);
    }

    public boolean save() {
        return ConfigFilesService.getInstance().saveAndKeepOldIfFails(SNIPPETS_FILE, file -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, bookmarkMap);
        });
    }

    public synchronized void addEntry(String id, String path) {
        if (id == null) {
            id = "";
        }
        load();
        List<String> bookmarks = bookmarkMap.get(id);
        if (bookmarks == null) {
            bookmarks = new ArrayList<>();
        }
        bookmarks.add(path);
        bookmarkMap.put(id, bookmarks);
        save();
    }

    public void addEntry(String id, List<String> path) {
        if (id == null) {
            id = "";
        }
        load();
        List<String> bookmarks = bookmarkMap.get(id);
        if (bookmarks == null) {
            bookmarks = new ArrayList<>();
        }
        bookmarks.addAll(path);
        bookmarkMap.put(id, bookmarks);
        save();
    }

    public List<String> getBookmarks(String id) {
        if (id == null) {
            id = "";
        }
        load();
        
        List<String> bookmarks = bookmarkMap.get(id);
        if (bookmarks != null) {
            return new ArrayList<>(bookmarks);
        }
        return new ArrayList<>();
    }
}
