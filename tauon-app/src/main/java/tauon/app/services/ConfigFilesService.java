package tauon.app.services;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.InitializationException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.util.misc.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public class ConfigFilesService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConfigFilesService.class);
    
    
    private static ConfigFilesService INSTANCE = null;
    
    private File directory;
    private File tempdirectory;
    private File backupDirectory;
    
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
            LOG.error("The default config directory for tauon cannot be created: {}", directory);
            throw new InitializationException();
        }
        
        tempdirectory = new File(directory, ".temp");
        if(!tempdirectory.exists() && !tempdirectory.mkdirs()) {
            LOG.error("The temp config directory for tauon cannot be created: {}", tempdirectory);
            throw new InitializationException();
        }
        
        backupDirectory = new File(directory, "backup");
        if(!backupDirectory.exists() && !backupDirectory.mkdirs()) {
            LOG.error("The backup config directory for tauon cannot be created: {}", backupDirectory);
            throw new InitializationException();
        }
        
    }
    
    
    private static void validateDefaultConfigPath() throws InitializationException {
        
        if (!Constants.CONFIG_DIR.exists()) {
            //Validate if the config directory can be created
            if (!Constants.CONFIG_DIR.mkdirs()) {
                LOG.error("The default config directory for tauon cannot be created: {}", Constants.CONFIG_DIR);
                throw new InitializationException();
            }
            
            // Check for old dirs
            for (File oldDir: Constants.OLD_CONFIG_DIRS){
                if(oldDir.exists()){
                    try {
                        FileUtils.copyDirectory(oldDir, Constants.CONFIG_DIR);
                        return; // Break execution
                    } catch (IOException e) {
                        LOG.error("The copy to the new directory failed: {}", oldDir, e);
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
                LOG.error("The config directory set by user for tauon doesn't exist: {}", tauonPath);
                throw new InitializationException();
            }
            
            LOG.info("User set a custom config directory: {}", tauonPath);
            return tauonPath;
        }
        
        return null;
    }
    
//    public void exportTo(File file) throws IOException {
//        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
//            for (File f : Objects.requireNonNull(directory.listFiles())) {
//                ZipEntry ent = new ZipEntry(f.getName());
//                out.putNextEntry(ent);
//                out.write(Files.readAllBytes(f.toPath()));
//                out.closeEntry();
//            }
//        }
//    }
    
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
        
        try {
            
            File realFile1 = new File(directory, file1);
            boolean realFile1Existed = realFile1.exists();
            
            if(realFile1Existed) {
                tempFile1 = File.createTempFile("tauontemp_save1_", file1, tempdirectory);
                tempFile1.deleteOnExit();
                
                try {
                    // Copy real 1 into temp 1
                    Files.copy(realFile1.toPath(), tempFile1.toPath(), REPLACE_EXISTING);
                } catch (Exception copyFile1Exception) {
                    LOG.error("Error while copying file1 ({}) into temp1 ({})", realFile1, tempFile1, copyFile1Exception);
                    throw copyFile1Exception;
                }
            }
            
            File realFile2 = new File(directory, file2);
            boolean realFile2Existed = realFile1.exists();
            
            if(realFile2Existed) {
                tempFile2 = File.createTempFile("tauontemp_save2_", file2, tempdirectory);
                tempFile2.deleteOnExit();
                
                try {
                    // Copy real 1 into temp 1
                    Files.copy(realFile2.toPath(), tempFile2.toPath(), REPLACE_EXISTING);
                } catch (Exception copyFile1Exception) {
                    LOG.error("Error while copying file2 ({}) into temp2 ({})", realFile1, tempFile1, copyFile1Exception);
                    throw copyFile1Exception;
                }
                
            }
            try {
                consumer.consumeFile(realFile1, realFile2);
            }catch (Exception exceptionWhileWriting){
                LOG.error("Error while writing files.", exceptionWhileWriting);
                
                // If it fails, restore temp 1 into real 1
                if (realFile1Existed) {
                    try {
                        Files.copy(tempFile1.toPath(), realFile1.toPath(), REPLACE_EXISTING);
                    } catch (Exception e2) {
                        LOG.error("Error while restoring file1 ({}) from temp1 ({})", tempFile1, realFile1, e2);
                    }
                } else {
                    // Real file 1 not existed, delete it
                    Files.deleteIfExists(realFile1.toPath());
                }
                
                // If it fails, restore temp 2 into real 2
                if (realFile2Existed) {
                    try {
                        Files.copy(tempFile2.toPath(), realFile2.toPath(), REPLACE_EXISTING);
                    } catch (Exception e2) {
                        LOG.error("Error while restoring file2 ({}) from temp2 ({})", tempFile2, realFile2, e2);
                    }
                } else {
                    // Real file 2 not existed, delete it
                    Files.deleteIfExists(realFile2.toPath());
                }
                
                throw exceptionWhileWriting;
            }
            
            return true; // Return true if not failed
            
        } catch (Exception e) {
            LOG.error("Error while saving sessions.", e);
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
            // Backup file1 (maybe will be overwritten by any future caller)
            File backup = new File(backupDirectory, System.currentTimeMillis() + "_" + file);
            
            try{
                Files.copy(file1.toPath(), backup.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backuping {} to {}", file, backup, e);
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
            // Backup file1 (maybe will be overwritten by any future caller)
            File backup1 = new File(backupDirectory, System.currentTimeMillis() + "_" + file1);
            File backup2 = new File(backupDirectory, System.currentTimeMillis() + "_" + file2);
            
            try{
                Files.copy(file1a.toPath(), backup1.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backing up {} to {}", file1a, backup1, e);
            }
            
            try{
                Files.copy(file2a.toPath(), backup2.toPath(), REPLACE_EXISTING);
            }catch (Exception e){
                LOG.error("Error while backing up {} to {}", file2a, backup2, e);
            }
            
            return false;
        }
        return true;
    }
    
    public File getFile(String file) {
        return new File(directory, file);
    }
    
    public File getDirectory() {
        return directory;
    }
    
    public File getBackupDirectory() {
        return backupDirectory;
    }
    
    public interface FileConsumer {
        void consumeFile(File file) throws Exception;
    }
    
    public interface BiFileConsumer {
        void consumeFile(File file1, File file2) throws Exception;
    }
    
}
