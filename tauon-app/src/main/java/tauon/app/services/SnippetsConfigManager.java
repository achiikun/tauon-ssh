/**
 *
 */
package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ui.containers.session.pages.terminal.snippets.SnippetItem;

import java.util.ArrayList;
import java.util.List;

import static tauon.app.util.misc.Constants.SNIPPETS_FILE;

/**
 * @author subhro
 *
 */
public class SnippetsConfigManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnippetsConfigManager.class);
    
    private static SnippetsConfigManager INSTANCE = null;
    
    private boolean loaded = false;
    private List<SnippetItem> snippetItems = new ArrayList<>();
    
    private SnippetsConfigManager() {
    
    }
    
    public static SnippetsConfigManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new SnippetsConfigManager();
        }
        return INSTANCE;
    }
    
    public synchronized boolean saveSnippets() {
        return ConfigFilesService.getInstance().saveAndKeepOldIfFails(SNIPPETS_FILE, file -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, snippetItems);
        });
    }

    public synchronized List<SnippetItem> getSnippetItems() {
        if(!loaded){
            
            loaded = true;
            
            boolean success = ConfigFilesService.getInstance().loadOrBackup(SNIPPETS_FILE, file -> {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if (file.exists()) {
                    snippetItems = objectMapper.readValue(file, new TypeReference<>() {});
                }
            });
            
            if(!success)
                LOG.error("Error loading snippets.");
            
        }
        
        return snippetItems;
    }

}
