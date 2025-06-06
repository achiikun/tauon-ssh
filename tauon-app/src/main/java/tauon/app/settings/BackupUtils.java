package tauon.app.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.ConfigFilesService;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static tauon.app.services.LanguageService.getBundle;

public class BackupUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BackupUtils.class);
    
    public static synchronized void backupConfigFilesToZip(Component appWindows, Set<String> filesToZip) throws OperationCancelledException {
        NativeFileChooser jfc = new NativeFileChooser();
        
        if (jfc.showSaveDialog(appWindows) != JFileChooser.APPROVE_OPTION) {
            throw new OperationCancelledException();
        }
        
        File file = jfc.getSelectedFile();
        
        // Ensure the file has a .zip extension
        if (!file.getName().toLowerCase().endsWith(".zip")) {
            file = new File(file.getAbsolutePath() + ".zip");
        }
        
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            for (File f : Objects.requireNonNull(ConfigFilesService.getInstance().getDirectory().listFiles())) {
                if (filesToZip.contains(f.getName())) {
                    ZipEntry ent = new ZipEntry(f.getName());
                    out.putNextEntry(ent);
                    out.write(Files.readAllBytes(f.toPath()));
                    out.closeEntry();
                }
            }
            
            AlertDialogUtils.showSuccess(
                    appWindows,
                    getBundle().getString("app.settings.message.export_success")
            );
            
        } catch (IOException e) {
            LOG.error("Error exporting settings", e);
            AlertDialogUtils.showError(
                    appWindows,
                    FormatUtils.$$(
                            getBundle().getString("app.settings.message.export_failed"),
                            Map.of("MESSAGE", e.getMessage(), "FILE", file.getName())
                    )
            );
        }
    }
    
    public static void importConfigFilesFromZip(Component parent, Set<String> filesInZip) throws OperationCancelledException, AlreadyFailedException {
        NativeFileChooser jfc = new NativeFileChooser();
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("ZIP Files (*.zip)", "zip"));
        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            throw new OperationCancelledException();
        }
        File f = jfc.getSelectedFile();
        
        if (!f.getName().toLowerCase().endsWith(".zip")) {
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_not_a_zip")
            );
            throw new AlreadyFailedException();
        }
        
        // Check if the file exists and is not empty
        if (!f.exists() || f.length() == 0) {
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        byte[] buffer = new byte[8192];
        
        Set<String> filesInBackup = new HashSet<>();
        
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(f))) {
            ZipEntry ent;
            
            while ((ent = in.getNextEntry()) != null) { // Read all entries in the ZIP file
                
                if(filesInZip.contains(ent.getName())) {
                    
                    File file = new File(ConfigFilesService.getInstance().getBackupDirectory(), ent.getName());
                    
                    // Prevent directory traversal attack
                    if (!file.getCanonicalPath().startsWith(ConfigFilesService.getInstance().getDirectory().getCanonicalPath())) {
                        LOG.error("ZIP entry is outside target directory: {}", ent.getName());
                        continue;
                    }
                    
                    try (OutputStream out = new FileOutputStream(file)) {
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    in.closeEntry();
                    
                    filesInBackup.add(ent.getName());
                }else if(!Constants.ALL_CONFIG_FILES.contains(ent.getName())){
                    AlertDialogUtils.showError(
                            parent,
                            getBundle().getString("app.settings.message.import_invalid_file")
                    );
                    throw new AlreadyFailedException();
                }
            }
            
        } catch (IOException e) {
            LOG.error("Error processing ZIP file: {}", e.getMessage(), e);
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        if(filesInZip.isEmpty()){
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_invalid_file")
            );
            throw new AlreadyFailedException();
        }
        
        if (JOptionPane.showConfirmDialog(
                parent,
                getBundle().getString("app.settings.message.import_alert_data_will_be_replaced")
        ) != JOptionPane.YES_OPTION) {
            throw new OperationCancelledException();
        }
        
        try {
            
            for(String fileInZip: filesInZip){
                if(filesInBackup.contains(fileInZip)){
                    Files.copy(
                            new File(ConfigFilesService.getInstance().getBackupDirectory(), fileInZip).toPath(),
                            new File(ConfigFilesService.getInstance().getDirectory(), fileInZip).toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }else{
                    Files.deleteIfExists(new File(ConfigFilesService.getInstance().getDirectory(), fileInZip).toPath());
                }
            }
            
            AlertDialogUtils.showSuccess(
                    parent,
                    getBundle().getString("app.settings.message.import_success")
            );
            
        }catch (IOException e){
            LOG.error("Error restoring files: {}", e.getMessage(), e);
            AlertDialogUtils.showError(
                    parent,
                    getBundle().getString("app.settings.message.import_error_generic")
            );
            throw new AlreadyFailedException();
        }
        
//        byte[] b = new byte[8192];
//        try (ZipInputStream in = new ZipInputStream(new FileInputStream(f))) {
//            ZipEntry ent = in.getNextEntry();
//            if(ent != null) {
//                File file = new File(ConfigFilesService.getInstance().getDirectory(), ent.getName());
//                try (OutputStream out = new FileOutputStream(file)) {
//                    while (true) {
//                        int x = in.read(b);
//                        if (x == -1)
//                            break;
//                        out.write(b, 0, x);
//                    }
//                }
//            }
//        } catch (IOException e) {
//            LOG.error("Error while importing zip.", e);
//            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_generic"));
//            throw new AlreadyFailedException();
//        }
    }
    
}
