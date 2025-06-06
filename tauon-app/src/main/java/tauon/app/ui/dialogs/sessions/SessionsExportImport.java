package tauon.app.ui.dialogs.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.services.ConfigFilesService;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SiteInfo;
import tauon.app.services.SitesConfigManager;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.settings.importers.SSHConfigImporter;
import tauon.app.ui.utils.AlertDialogUtils;
import tauon.app.util.misc.Constants;
import tauon.app.util.misc.FormatUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static tauon.app.services.LanguageService.getBundle;

public class SessionsExportImport {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionsExportImport.class);
    
//    public static synchronized void exportSessions(Component appWindows) throws OperationCancelledException {
//        NativeFileChooser jfc = new NativeFileChooser();
//
//        if (jfc.showSaveDialog(appWindows) != JFileChooser.APPROVE_OPTION) {
//            throw new OperationCancelledException();
//        }
//
//        File file = jfc.getSelectedFile();
//
//        // Ensure the file has a .zip extension
//        if (!file.getName().toLowerCase().endsWith(".zip")) {
//            file = new File(file.getAbsolutePath() + ".zip");
//        }
//
//        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
//            for (File f : Objects.requireNonNull(ConfigFilesService.getInstance().getDirectory().listFiles())) {
//                ZipEntry ent = new ZipEntry(f.getName());
//                out.putNextEntry(ent);
//                out.write(Files.readAllBytes(f.toPath()));
//                out.closeEntry();
//            }
//
//            AlertDialogUtils.showSuccess(
//                    appWindows,
//                    getBundle().getString("app.settings.message.export_success")
//            );
//
//        } catch (IOException e) {
//            LOG.error("Error exporting settings", e);
//            AlertDialogUtils.showError(
//                    appWindows,
//                    FormatUtils.$$(
//                            getBundle().getString("app.settings.message.export_failed"),
//                            Map.of("MESSAGE", e.getMessage(), "FILE", file.getName())
//                    )
//            );
//        }
//    }

//    public static synchronized void importSessionsFromTauonZip(Component parent) throws OperationCancelledException, AlreadyFailedException {
//        NativeFileChooser jfc = new NativeFileChooser();
//        jfc.addChoosableFileFilter(new FileNameExtensionFilter("ZIP Files (*.zip)", "zip"));
//        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
//            throw new OperationCancelledException();
//        }
//        File f = jfc.getSelectedFile();
//
//        if (!f.getName().toLowerCase().endsWith(".zip")) {
//            AlertDialogUtils.showError(
//                    parent,
//                    getBundle().getString("app.sites.import_dialog.not_a_zip")
//            );
//            throw new AlreadyFailedException();
//        }
//
//        // Check if the file exists and is not empty
//        if (!f.exists() || f.length() == 0) {
//            AlertDialogUtils.showError(
//                    parent,
//                    getBundle().getString("app.sites.import_dialog.invalid_file")
//            );
//            throw new AlreadyFailedException();
//        }
//
//        if (JOptionPane.showConfirmDialog(
//                parent,
//                getBundle().getString("app.sites.import_dialog.alert_data_will_be_replaced")
//        ) != JOptionPane.YES_OPTION) {
//            throw new OperationCancelledException();
//        }
//
//        byte[] buffer = new byte[8192];
//
//        try (ZipInputStream in = new ZipInputStream(new FileInputStream(f))) {
//            ZipEntry ent;
//
//            while ((ent = in.getNextEntry()) != null) { // Read all entries in the ZIP file
//                File file = new File(ConfigFilesService.getInstance().getDirectory(), ent.getName());
//
//                // Prevent directory traversal attack
//                if (!file.getCanonicalPath().startsWith(ConfigFilesService.getInstance().getDirectory().getCanonicalPath())) {
//                    LOG.error("ZIP entry is outside target directory: {}", ent.getName());
//                    continue;
//                }
//
//                try (OutputStream out = new FileOutputStream(file)) {
//                    int bytesRead;
//                    while ((bytesRead = in.read(buffer)) != -1) {
//                        out.write(buffer, 0, bytesRead);
//                    }
//
//                }
//                in.closeEntry();
//            }
//
//        } catch (IOException e) {
//            LOG.error("Error processing ZIP file: {}", e.getMessage(), e);
//            AlertDialogUtils.showError(
//                    parent,
//                    getBundle().getString("app.sites.import_dialog.invalid_file")
//            );
//            throw new AlreadyFailedException();
//        }
//
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
//    }

//    public static synchronized void importSessionsSSHConfig(Component parent) throws OperationCancelledException, AlreadyFailedException {
//        NativeFileChooser jfc = new NativeFileChooser();
//        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
//            throw new OperationCancelledException();
//        }
//        File f = jfc.getSelectedFile();
//
//        if(f == null || !f.canRead()){
//            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_file_not_found"));
//            throw new AlreadyFailedException();
//        }
//
//        DefaultComboBoxModel<Constants.ConflictAction> conflictOptionsCmb = new DefaultComboBoxModel<>(Constants.ConflictAction.values());
//        conflictOptionsCmb.removeAllElements();
//        for (Constants.ConflictAction conflictActionCmb : Constants.ConflictAction.values()) {
//            if (conflictActionCmb.getKey() < 3) {
//                conflictOptionsCmb.addElement(conflictActionCmb);
//            }
//        }
//
//        JComboBox<Constants.ConflictAction> cmbOptionsExistingInfo = new JComboBox<>(conflictOptionsCmb);
//
//        if (JOptionPane.showOptionDialog(
//                parent,
//                new Object[]{
//                        getBundle().getString("app.sites.import_dialog.action_for_repeated_sessions"),
//                        cmbOptionsExistingInfo
//                },
//                getBundle().getString("app.sites.import_dialog.title"),
//                JOptionPane.OK_CANCEL_OPTION,
//                JOptionPane.PLAIN_MESSAGE,
//                null,
//                null,
//                null
//        ) != JOptionPane.OK_OPTION) {
//            throw new OperationCancelledException();
//        }
//
//        int imported = 0;
//        int skipped = 0;
//        int overwritten = 0;
//
//        List<SiteInfo> sessions;
//
//        try {
//            sessions = SSHConfigImporter.getSessionsFromFile(f);
//        } catch (FileNotFoundException e) {
//            LOG.error("Error while importing sessions.", e);
//            AlertDialogUtils.showError(parent, getBundle().getString("app.sites.import_dialog.error_file_not_found"));
//            throw new AlreadyFailedException();
//        }
//
//        SavedSessionTree tree = SitesConfigManager.getInstance().getSessionTree(new PasswordPromptHelper(parent));
//            SessionFolder folder = tree.getFolder();
//
//            List<SessionFolder> folders = folder.getFolders();
//            int total = sessions.size();
//            SessionFolder sessionFolder;
//            for (SiteInfo session : sessions) {
//                session.setId(UUID.randomUUID().toString());
//
//                sessionFolder = new SessionFolder();
//                sessionFolder.setId(UUID.randomUUID().toString());
//                sessionFolder.setName(session.getName());
//
//                List<SiteInfo> item = new ArrayList<>();
//                item.add(session);
//                sessionFolder.setItems(item);
//
//                if (folders.contains(sessionFolder)) {
//                    if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.SKIP) {
//                        continue;
//                    }
//                    if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.AUTORENAME) {
//                        sessionFolder.setName("Copy of " + sessionFolder.getName());
//                        folders.add(sessionFolder);
//                    } else if (cmbOptionsExistingInfo.getSelectedItem() == Constants.ConflictAction.OVERWRITE) {
//                        folders.set(folders.indexOf(sessionFolder), sessionFolder);
//                    }
//                    imported++;
//                    continue;
//                }
//
//                SitesConfigManager.getInstance().setPasswordsFrom(session);
//                folders.add(sessionFolder);
//                imported++;
//            }
//
//            folder.setFolders(folders);
//
//            SitesConfigManager.getInstance().save(new PasswordPromptHelper(parent));
//
//            JOptionPane.showMessageDialog(parent,
//                    FormatUtils.$$(
//                            getBundle().getString("app.sites.import_dialog.result.content"),
//                            Map.of(
//                                    "total", total,
//                                    "imported", imported,
//                                    "skipped", skipped,
//                                    "overwritten", overwritten
//                            )
//                    ),
//                    getBundle().getString("app.sites.import_dialog.result.title"),
//                    JOptionPane.INFORMATION_MESSAGE
//            );
//
//    }

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
