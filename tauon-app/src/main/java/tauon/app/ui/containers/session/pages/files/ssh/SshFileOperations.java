package tauon.app.ui.containers.session.pages.files.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.services.SettingsService;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.util.misc.PathUtils;
import tauon.app.util.ssh.SudoUtils;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static tauon.app.services.LanguageService.getBundle;

public class SshFileOperations {
    private static final Logger LOG = LoggerFactory.getLogger(SshFileOperations.class);

    public SshFileOperations() {
    }

    public static void deleteUsingRMRF(List<FileInfo> files, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {

        StringBuilder sb = new StringBuilder("rm -rf ");

        for (FileInfo file : files) {
            sb.append("\"").append(file.getPath()).append("\" ");
        }
        
        int ret;
        if ((ret = instance.exec(sb.toString(), new AtomicBoolean(false))) != 0) {
            throw new RemoteOperationException.ErrorReturnCode(sb.toString(), ret);
        }
    }

    public boolean moveTo(TauonRemoteSessionInstance instance, List<FileInfo> files, String targetFolder, FileSystem fs) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException {
        List<FileInfo> fileList = fs.list(targetFolder);
        List<FileInfo> dupList = new ArrayList<>();
        for (FileInfo file : files) {
            for (FileInfo file1 : fileList) {
                if (file.getName().equals(file1.getName())) {
                    dupList.add(file);
                }
            }
        }

        int action = -1;
        if (!dupList.isEmpty()) {
            // TODO i18n
            JComboBox<String> cmbs = new JComboBox<>(
                    new String[]{"Auto rename", "Overwrite"});
            if (JOptionPane.showOptionDialog(null, new Object[]{
                            "Some file with the same name already exists. Please choose an action",
                            cmbs}, "Action required", JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.YES_OPTION) {
                action = cmbs.getSelectedIndex();
            } else {
                return false;
            }
        }

        StringBuilder command = new StringBuilder();
        for (FileInfo fileInfo : files) {
            if (fileInfo.getType() == FileType.DIR_LINK
                    || fileInfo.getType() == FileType.DIR) {
                command.append("mv ");
            } else {
                command.append("mv -T ");
            }
            command.append("\"").append(fileInfo.getPath()).append("\" ");
            if (dupList.contains(fileInfo) && action == 0) {
                command.append("\"").append(PathUtils.combineUnix(targetFolder,
                        getUniqueName(fileList, fileInfo.getName()))).append("\"; ");
            } else {
                command.append("\"").append(PathUtils.combineUnix(targetFolder,
                        fileInfo.getName())).append("\"; ");
            }
        }

        System.out.println("Move: " + command);
        if (instance.exec(command.toString(), new AtomicBoolean(false)) != 0) {
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.access_denied"));
                return false;
            }
            
            // TODO i18n
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, rename using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                if (!instance.isSessionClosed()) {
                    JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                }
                return false;
            }

            int ret = SudoUtils.runSudo(command.toString(), instance);
            if (ret != -1) {
                return ret == 0;
            }

            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        } else {
            return true;
        }
        return false;
    }

    public boolean copyTo(
            TauonRemoteSessionInstance instance,
            List<FileInfo> files,
            String targetFolder,
            FileSystem fs
    ) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException {
        List<FileInfo> fileList = fs.list(targetFolder);
        List<FileInfo> dupList = new ArrayList<>();
        for (FileInfo file : files) {
            for (FileInfo file1 : fileList) {
                if (file.getName().equals(file1.getName())) {
                    dupList.add(file);
                }
            }
        }

        int action = -1;
        if (!dupList.isEmpty()) {
            // TODO i18n
            JComboBox<String> cmbs = new JComboBox<>(
                    new String[]{"Auto rename", "Overwrite"});
            if (JOptionPane.showOptionDialog(null, new Object[]{
                            "Some file with the same name already exists. Please choose an action",
                            cmbs}, getBundle().getString("general.message.action_required"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.YES_OPTION) {
                action = cmbs.getSelectedIndex();
            } else {
                return false;
            }
        }

        StringBuilder command = new StringBuilder();
        for (FileInfo fileInfo : files) {
            if (fileInfo.getType() == FileType.DIR_LINK
                    || fileInfo.getType() == FileType.DIR) {
                command.append("cp -rf ");
            } else {
                command.append("cp -Tf ");
            }
            command.append("\"").append(fileInfo.getPath()).append("\" ");
            if (dupList.contains(fileInfo) && action == 0) {
                command.append("\"").append(PathUtils.combineUnix(targetFolder,
                        getUniqueName(fileList, fileInfo.getName()))).append("\"; ");
            } else {
                command.append("\"").append(PathUtils.combineUnix(targetFolder,
                        fileInfo.getName())).append("\"; ");
            }
        }

        System.out.println("Copy: " + command);
        if (instance.exec(command.toString(), new AtomicBoolean(false)) != 0) {
            // TODO i18n
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, "Access denied");
                return false;
            }
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, copy using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                if (!instance.isSessionClosed()) {
                    JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                }
                return false;
            }

            int ret = SudoUtils.runSudo(command.toString(), instance);
            if (ret != -1) {
                return ret == 0;
            }

            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        } else {
            return true;
        }
        return false;
    }

    private String getUniqueName(List<FileInfo> list, String name) {
        while (true) {
            boolean found = false;
            for (FileInfo f : list) {
                if (name.equals(f.getName())) {
                    name = "Copy of " + name;
                    found = true;
                    break;
                }
            }
            if (!found)
                break;
        }
        return name;
    }

    public boolean rename(String oldName, String newName, FileSystem fs,
                          TauonRemoteSessionInstance instance) throws TauonOperationException, OperationCancelledException, SessionClosedException, InterruptedException {
        try {
            fs.rename(oldName, newName);
            return true;
        } catch (RemoteOperationException.PermissionDenied e) {
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.access_denied"));
                return false;
            }
            
            // TODO i18n
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, rename using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                return renameWithPrivilege(oldName, newName, instance);
            }

            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
            return false;
        }
//        catch (Exception e) {
//            e.printStackTrace();
//            if (!instance.isSessionClosed()) {
//                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
//            }
//            return false;
//        }
    }

    private boolean renameWithPrivilege(String oldName, String newName,
                                        TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        StringBuilder command = new StringBuilder();
        command.append("mv \"").append(oldName).append("\" \"").append(newName).append("\"");
        System.out.println("Invoke sudo: " + command);
        int ret = SudoUtils.runSudo(command.toString(), instance);
        if (ret == -1) {
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        }
        return ret == 0;
    }

    public boolean delete(FileInfo[] targetList, FileSystem fs, TauonRemoteSessionInstance instance) throws TauonOperationException, OperationCancelledException, SessionClosedException, InterruptedException {
        try {
            try {
                // Try to remove it using "rm -rf" because it's faster than sftp
                deleteUsingRMRF(Arrays.asList(targetList), instance);
                return true;
//            } catch (FileNotFoundException e) {
//                System.out.println("delete: file not found");
//                e.printStackTrace();
//                throw e;
            } catch (RemoteOperationException e) {
                // Fallback to sftp
                for (FileInfo s : targetList) {
                    fs.delete(s);
                }
                return true;
            }
        } catch (RemoteOperationException.FileNotFound | RemoteOperationException.PermissionDenied e) {
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.access_denied"));
                return false;
            }
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, delete using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                return deletePrivilege(targetList, instance);
            }
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
            return false;

        }
    }

    private boolean deletePrivilege(FileInfo[] targetList, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        StringBuilder sb = new StringBuilder("rm -rf ");
        for (FileInfo file : targetList) {
            sb.append("\"").append(file.getPath()).append("\" ");
        }

        System.out.println("Invoke sudo: " + sb);
        int ret = SudoUtils.runSudo(sb.toString(), instance);
        if (ret == -1) {
            JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
        }
        return ret == 0;
    }

    public boolean newFile(FileInfo[] files, FileSystem fs, String folder, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String text = JOptionPane.showInputDialog("New file");
        if (text == null || text.isEmpty()) {
            return false;
        }
        boolean alreadyExists = false;
        for (FileInfo f : files) {
            if (f.getName().equals(text)) {
                alreadyExists = true;
                break;
            }
        }
        if (alreadyExists) {
            JOptionPane.showMessageDialog(null, getBundle().getString("app.files.message.file_exists"));
            return false;
        }
        try {
            fs.createFile(PathUtils.combineUnix(folder, text));
            return true;
        } catch (RemoteOperationException.PermissionDenied e1) {
            e1.printStackTrace();
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.access_denied"));
                return false;
            }
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, new file using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (!touchWithPrivilege(folder, text, instance)) {
                    if (!instance.isSessionClosed()) {
                        JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                    }
                    return false;
                }
                return true;
            }
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }

            return false;
        } catch (Exception e1) {
            e1.printStackTrace();
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        }
        return false;
    }

    private boolean touchWithPrivilege(String path, String newFile, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String file = PathUtils.combineUnix(path, newFile);
        StringBuilder command = new StringBuilder();
        command.append("touch \"").append(file).append("\"");
        System.out.println("Invoke sudo: " + command);
        int ret = SudoUtils.runSudo(command.toString(), instance);
        if (ret == -1) {
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        }
        return ret == 0;
    }

    public boolean newFolder(FileInfo[] files, String folder, FileSystem fs, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String text = JOptionPane.showInputDialog("New folder name");
        if (text == null || text.length() < 1) {
            return false;
        }
        boolean alreadyExists = false;
        for (FileInfo f : files) {
            if (f.getName().equals(text)) {
                alreadyExists = true;
                break;
            }
        }
        if (alreadyExists) {
            // TODO i18n
            JOptionPane.showMessageDialog(null,
                    "File with same name already exists");
            return false;
        }
        try {
            fs.mkdir(PathUtils.combineUnix(folder, text));
            return true;
        } catch (RemoteOperationException.PermissionDenied e1) {
            e1.printStackTrace();
            if (!SettingsService.getSettings().isUseSudo()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.access_denied"));
                return false;
            }
            if (!SettingsService.getSettings().isPromptForSudo()
                    || JOptionPane.showConfirmDialog(null,
                    "Access denied, try using sudo?", getBundle().getString("general.message.ask_use_sudo"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (!mkdirWithPrivilege(folder, text, instance)) {
                    if (!instance.isSessionClosed()) {
                        JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
                    }
                    return false;
                }
                return true;
            }
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
            return false;

        } catch (Exception e1) {
            e1.printStackTrace();
            if (!instance.isSessionClosed()) {
                JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
            }
        }
        return false;
    }

    private boolean mkdirWithPrivilege(String path, String newFolder, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String file = PathUtils.combineUnix(path, newFolder);
        StringBuilder command = new StringBuilder();
        command.append("mkdir \"").append(file).append("\"");
        System.out.println("Invoke sudo: " + command);
        int ret = SudoUtils.runSudo(command.toString(), instance);
        if (ret == -1 && !instance.isSessionClosed()) {
            JOptionPane.showMessageDialog(null, getBundle().getString("general.message.operation_failed"));
        }
        return ret == 0;
    }

    public boolean createLink(FileInfo[] files, FileSystem fs,
                              TauonRemoteSessionInstance instance) {
        JTextField txtLinkName = new SkinnedTextField(30);
        JTextField txtFileName = new SkinnedTextField(30);
        JCheckBox chkHardLink = new JCheckBox("Hardlink");

        if (files.length > 0) {
            FileInfo info = files[0];
            txtLinkName.setText(
                    PathUtils.combineUnix(PathUtils.getParent(info.getPath()),
                            "Link to " + info.getName()));
            txtFileName.setText(info.getPath());
        }

        if (JOptionPane.showOptionDialog(null,
                new Object[]{"Create link", "Link path", txtLinkName,
                        "File name", txtFileName, chkHardLink},
                "Create link", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.OK_OPTION) {
            if (!txtLinkName.getText().isEmpty()
                    && !txtFileName.getText().isEmpty()) {
                return createLinkAsync(txtFileName.getText(),
                        txtLinkName.getText(), chkHardLink.isSelected(), fs);
            }
        }
        return false;
    }

    private boolean createLinkAsync(String src, String dst, boolean hardLink,
                                    FileSystem fs) {
        try {
            fs.createLink(src, dst, hardLink);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
