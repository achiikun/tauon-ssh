package tauon.app.services;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.InitializationException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.util.misc.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public class ConfigFilesService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConfigFilesService.class);
    
    private static ConfigFilesService INSTANCE = null;
    
    private File directory;
    private File tempdirectory;
    private File backupdirectory;
    
    public static ConfigFilesService getInstance() {
        if(INSTANCE == null){
            INSTANCE = new ConfigFilesService();
        }
        return INSTANCE;
    }
    
    private ConfigFilesService(){
        
    }
    
    public void initialize() throws InitializationException {
        String custom = validateCustomMuonPath();
        if(custom != null) {
            directory = new File(custom);
        }else{
            directory = Constants.CONFIG_DIR;
            validateDefaultConfigPath();
        }
        
        if(!directory.exists() && !directory.mkdirs()) {
            LOG.error("The default config directory for tauon cannot be created: " + directory);
            throw new InitializationException();
        }
        
        tempdirectory = new File(directory, ".temp");
        if(!tempdirectory.exists() && !tempdirectory.mkdirs()) {
            LOG.error("The temp config directory for tauon cannot be created: " + tempdirectory);
            throw new InitializationException();
        }
        
        backupdirectory = new File(directory, "backup");
        if(!backupdirectory.exists() && !backupdirectory.mkdirs()) {
            LOG.error("The backup config directory for tauon cannot be created: " + backupdirectory);
            throw new InitializationException();
        }
        
    }
    
    
    private static void validateDefaultConfigPath() throws InitializationException {
        
        if (!Constants.CONFIG_DIR.exists()) {
            //Validate if the config directory can be created
            if (!Constants.CONFIG_DIR.mkdirs()) {
                LOG.error("The default config directory for tauon cannot be created: " + Constants.CONFIG_DIR);
                throw new InitializationException();
            }
            
            // Check for old dirs
            for (File oldDir: Constants.OLD_CONFIG_DIRS){
                if(oldDir.exists()){
                    try {
                        FileUtils.copyDirectory(oldDir, Constants.CONFIG_DIR);
                        return; // Break execution
                    } catch (IOException e) {
                        LOG.error("The copy to the new directory failed: " + oldDir, e);
                        throw new InitializationException();
                    }
                }
            }
            
        }
        
    }
    
    
    private static String validateCustomMuonPath() throws InitializationException {
        //Checks if the parameter muonPath is set in the startup
        
        String tauonPath = System.getProperty("tauonPath");
        if(tauonPath == null || tauonPath.isBlank())
            tauonPath = System.getProperty("muonPath");
        
        if (tauonPath != null && !tauonPath.isEmpty()) {
            //Validate if the config directory can be created
            if (!Paths.get(tauonPath).toFile().exists()) {
                LOG.error("The config directory set by user for tauon doesn't exist: " + tauonPath);
                throw new InitializationException();
            }
            
            LOG.info("User set a custom config directory: " + tauonPath);
            return tauonPath;
        }
        
        return null;
    }
    
    public void exportTo(File file) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            for (File f : directory.listFiles()) {
                ZipEntry ent = new ZipEntry(f.getName());
                out.putNextEntry(ent);
                out.write(Files.readAllBytes(f.toPath()));
                out.closeEntry();
            }
        }
    }
    
    public boolean saveAndKeepOldIfFails(String file, FileConsumer consumer) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tauontemp_save_", file, tempdirectory);
            tempFile.deleteOnExit();
            consumer.consumeFile(tempFile);
            Files.copy(tempFile.toPath(), new File(directory, file).toPath(), REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if(tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }
    
    public boolean saveAndKeepOldIfFails(String file1, String file2, BiFileConsumer consumer) {
        File tempFile1 = null;
        File tempFile2 = null;
        File tempFileOld1 = null;
        
        try {
            
            tempFile1 = File.createTempFile("tauontemp_save1_", file1, tempdirectory);
            tempFile1.deleteOnExit();
            
            tempFile2 = File.createTempFile("tauontemp_save2_", file2, tempdirectory);
            tempFile2.deleteOnExit();
            
            consumer.consumeFile(tempFile1, tempFile2);
            
            tempFileOld1 = File.createTempFile("tauontemp_save3_", file1, tempdirectory);
            tempFileOld1.deleteOnExit();
            
            Files.copy(new File(directory, file1).toPath(), tempFileOld1.toPath(), REPLACE_EXISTING);
            
            Files.copy(tempFile1.toPath(), new File(directory, file1).toPath(), REPLACE_EXISTING);
            
            try{
                Files.copy(tempFile2.toPath(), new File(directory, file2).toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                // If file2 fails, restore file1
                
                try{
                    Files.copy(tempFileOld1.toPath(), new File(directory, file1).toPath(), REPLACE_EXISTING);
                }catch (Exception e2){
                    LOG.error("Error while restoring file1", e2);
                }
                
                throw e;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if(tempFile1 != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile1.delete();
            }
            if(tempFile2 != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile2.delete();
            }
            if(tempFileOld1 != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFileOld1.delete();
            }
        }
        
    }
    
//    public boolean load(String file, FileConsumer consumer) {
//        // TODO
//        return true;
//    }
    
    public boolean loadOrBackup(String file, FileConsumer consumer) {
        File file1 = new File(directory, file);
        try {
            consumer.consumeFile(file1);
        } catch (Exception ignored) {
            // Backup file1 (maybe will be overwritten by caller)
            File backup = new File(backupdirectory, System.currentTimeMillis() + "_" + file);
            
            try{
                Files.copy(file1.toPath(), backup.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backuping " + file + " to " + backup, e);
            }
            
            return false;
        }
        return true;
    }
    
    public boolean loadOrBackupCancellable(String file1, String file2, BiFileConsumer consumer) throws OperationCancelledException {
        File file1a = new File(directory, file1);
        File file2a = new File(directory, file2);
        try {
            consumer.consumeFile(file1a, file2a);
        } catch (OperationCancelledException e){
            throw e;
        } catch (Exception ignored) {
            // Backup file1 (maybe will be overwritten by caller)
            File backup1 = new File(backupdirectory, System.currentTimeMillis() + "_" + file1);
            File backup2 = new File(backupdirectory, System.currentTimeMillis() + "_" + file2);
            
            try{
                Files.copy(file1a.toPath(), backup1.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backuping " + file1a + " to " + backup1, e);
            }
            
            try{
                Files.copy(file2a.toPath(), backup2.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backuping " + file2a + " to " + backup2, e);
            }
            
            return false;
        }
        return true;
    }
    
    public interface FileConsumer {
        void consumeFile(File file) throws Exception;
    }
    
    public interface BiFileConsumer {
        void consumeFile(File file1, File file2) throws Exception;
    }
    
}
