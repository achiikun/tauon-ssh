/**
 *
 */
package tauon.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.settings.Settings;
import tauon.app.ui.containers.main.BackgroundTransferPanel;
import tauon.app.ui.containers.session.pages.terminal.snippets.SnippetItem;
import tauon.app.util.misc.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.util.misc.Constants.SNIPPETS_FILE;
import static tauon.app.util.misc.Constants.CONFIG_DIR;

/**
 * @author subhro
 *
 */
public class SnippetManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnippetManager.class);
    
    private static SnippetManager INSTANCE = null;
    
    private boolean loaded = false;
    private List<SnippetItem> snippetItems = new ArrayList<>();
    
    private SnippetManager() {
    
    }
    
    public static SnippetManager getInstance() {
        if(INSTANCE == null){
            INSTANCE = new SnippetManager();
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
