package tauon.app.ui.dialogs.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SessionInfo;
import tauon.app.settings.SessionService;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.settings.importers.SSHConfigImporter;
import tauon.app.ui.utils.AlertDialogUtils;
import util.Constants;
import util.FormatUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static tauon.app.services.LanguageService.getBundle;
import static util.Constants.CONFIG_DIR;

public class SessionExportImport {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);
    
    public static synchronized void exportSessions() throws IOException {
    
    }

    public static synchronized void importSessions(Component parent) throws OperationCancelledException, AlreadyFailedException {
        NativeFileChooser jfc = new NativeFileChooser();
        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            throw new OperationCancelledException();
        }
        File f = jfc.getSelectedFile();
        
        if (JOptionPane.showConfirmDialog(parent,
                getBundle().getString("sessions.import.dialog.alert_data_will_be_replaced")
        ) != JOptionPane.YES_OPTION) {
            throw new OperationCancelledException();
        }
        
        byte[] b = new byte[8192];
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(f))) {
            ZipEntry ent = in.getNextEntry();
            if(ent != null) {
                File file = new File(CONFIG_DIR, ent.getName());
                try (OutputStream out = new FileOutputStream(file)) {
                    while (true) {
                        int x = in.read(b);
                        if (x == -1)
                            break;
                        out.write(b, 0, x);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error while importing zip.", e);
            AlertDialogUtils.showError(parent, getBundle().getString("sessions.import.dialog.error_generic"));
            throw new AlreadyFailedException();
        }
    }

    public static synchronized void importSessionsSSHConfig(Component parent) throws OperationCancelledException, AlreadyFailedException {
        NativeFileChooser jfc = new NativeFileChooser();
        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            throw new OperationCancelledException();
        }
        File f = jfc.getSelectedFile();
        
        if(f == null || !f.canRead()){
            AlertDialogUtils.showError(parent, getBundle().getString("sessions.import.dialog.error_file_not_found"));
            throw new AlreadyFailedException();
        }

        DefaultComboBoxModel<Constants.ConflictAction> conflictOptionsCmb = new DefaultComboBoxModel<>(Constants.ConflictAction.values());
        conflictOptionsCmb.removeAllElements();
        for (Constants.ConflictAction conflictActionCmb : Constants.ConflictAction.values()) {
            if (conflictActionCmb.getKey() < 3) {
                conflictOptionsCmb.addElement(conflictActionCmb);
            }
        }
        
        JComboBox<Constants.ConflictAction> cmbOptionsExistingInfo = new JComboBox<>(conflictOptionsCmb);

        if (JOptionPane.showOptionDialog(
                parent,
                new Object[]{
                        "In repeated sessions do:",
                        cmbOptionsExistingInfo
                },
                getBundle().getString("sessions.import.dialog.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        ) != JOptionPane.OK_OPTION) {
            throw new OperationCancelledException();
        }
        
        int imported = 0;
        int skipped = 0;
        int overwritten = 0;
        
        List<SessionInfo> sessions;
        
        try {
            sessions = SSHConfigImporter.getSessionFromFile(f);
        } catch (FileNotFoundException e) {
            LOG.error("Error while importing sessions.", e);
            AlertDialogUtils.showError(parent, getBundle().getString("sessions.import.dialog.error_file_not_found"));
            throw new AlreadyFailedException();
        }
        
        SavedSessionTree tree = SessionService.getInstance().getSessionTree(new PasswordPromptHelper(parent));
            SessionFolder folder = tree.getFolder();

            List<SessionFolder> folders = folder.getFolders();
            int total = sessions.size();
            SessionFolder sessionFolder;
            for (SessionInfo session : sessions) {
                session.setId(UUID.randomUUID().toString());
                
                sessionFolder = new SessionFolder();
                sessionFolder.setId(UUID.randomUUID().toString());
                sessionFolder.setName(session.getName());
                
                List<SessionInfo> item = new ArrayList<>();
                item.add(session);
                sessionFolder.setItems(item);
                
                if (folders.contains(sessionFolder)) {
                    if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.SKIP) {
                        continue;
                    }
                    if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.AUTORENAME) {
                        sessionFolder.setName("Copy of " + sessionFolder.getName());
                        folders.add(sessionFolder);
                    } else if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.OVERWRITE) {
                        folders.set(folders.indexOf(sessionFolder), sessionFolder);
                    }
                    imported++;
                    continue;
                }
                
                SessionService.getInstance().setPasswordsFrom(session);
                folders.add(sessionFolder);
                imported++;
            }

            folder.setFolders(folders);
            
            SessionService.getInstance().save(new PasswordPromptHelper(parent));
            
//            JOptionPane.showMessageDialog(parent,
//                    "Total=" + total +
//                            "\nImported=" + imported +
//                            "\nSkipped=" + skipped +
//                            "\noverwritten=" + overwritten, "Session information", JOptionPane.INFORMATION_MESSAGE);
            
            JOptionPane.showMessageDialog(parent,
                    FormatUtils.$$(
                            getBundle().getString("sessions.import.dialog.result.content"),
                            Map.of(
                                    "total", total,
                                    "imported", imported,
                                    "skipped", skipped,
                                    "overwritten", overwritten
                            )
                    ),
                    getBundle().getString("sessions.import.dialog.result.title"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            
    }

//    public static synchronized void importOnFirstRun() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        try {
//            SavedSessionTree savedSessionTree = objectMapper.readValue(
//                    new File(CONFIG_DIR + File.separator + SESSION_DB_FILE),
//                    new TypeReference<>() {
//                    });
//            save(savedSessionTree.getFolder(), savedSessionTree.getLastSelection(),
//                    new File(CONFIG_DIR, SESSION_DB_FILE));
////            Files.copy(Paths.get(configDir, SNIPPETS_FILE),
////                    Paths.get(configDir, SNIPPETS_FILE));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
