package tauon.app.ui.containers.session.pages.files.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.IStopper;
import tauon.app.ssh.SSHCommandRunner;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.util.misc.PathUtils;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ArchiveOperation {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveOperation.class);
    
    private final Map<String, String> extractCommands;
    private final Map<String, String> compressCommands;

    public ArchiveOperation() {
        extractCommands = new LinkedHashMap<>();
        extractCommands.put(".tar", "cat \"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.gz",
                "gunzip -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tgz", "gunzip -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.bz2",
                "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tbz2",
                "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tbz", "bzip2 -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".tar.xz", "xz -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".txz", "xz -d -c <\"%s\"|tar -C \"%s\" -xvf -");
        extractCommands.put(".zip", "unzip -o \"%s\" -d \"%s\" ");
        extractCommands.put(".gz", "gunzip -c < \"%s\" > \"%s\" ");
        extractCommands.put(".xz", "xz -d -c < \"%s\" > \"%s\" ");
        extractCommands.put(".bz2", "bzip2 -d -c  < \"%s\" > \"%s\" ");

        compressCommands = new LinkedHashMap<>();
        compressCommands.put("tar", "tar cvf - %s|cat>\"%s\"");
        compressCommands.put("tar.gz", "tar cvf - %s|gzip>\"%s\"");
        compressCommands.put("tar.bz2", "tar cvf - %s|bzip2 -z>\"%s\"");
        compressCommands.put("tar.xz", "tar cvf - %s|xz -z>\"%s\"");
        compressCommands.put("zip", "zip -r - %s|cat>\"%s\"");
    }

    public boolean isSupportedArchive(String fileName) {
        for (String key : extractCommands.keySet()) {
            if (fileName.endsWith(key)) {
                return true;
            }
        }
        return false;
    }

    public String getExtractCommand(String fileName) {
        for (String key : extractCommands.keySet()) {
            if (fileName.endsWith(key)) {
                return extractCommands.get(key);
            }
        }
        return null;
    }

    private boolean isSingleArchive(String archivePath) {
        archivePath = archivePath.toLowerCase(Locale.ENGLISH);
        for (String key : extractCommands.keySet()) {
            if (archivePath.endsWith(key) && (key.equals(".xz")
                    || key.equals(".gz") || key.equals(".bz2"))) {
                if (!(archivePath.endsWith(".tar.xz")
                        || archivePath.endsWith(".tar.gz")
                        || archivePath.endsWith(".tar.bz2"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getArchiveFileName(String archivePath) {
        String path = archivePath.toLowerCase(Locale.ENGLISH);
        if (path.endsWith(".gz") || path.endsWith(".xz")) {
            return archivePath.substring(0, archivePath.length() - 3);
        } else {
            return archivePath.substring(0, archivePath.length() - 4);
        }
    }

    public boolean extractArchive(SSHConnectionHandler client, String archivePath, String targetFolder, IStopper stopFlag) throws RemoteOperationException, OperationCancelledException, SessionClosedException, InterruptedException {
        String command = getExtractCommand(archivePath);
        if (command == null) {
            LOG.error("Unsupported file type: {}", archivePath);
            return false;
        }
        command = String
                .format(command, archivePath,
                        isSingleArchive(archivePath)
                                ? PathUtils.combineUnix(targetFolder,
                                getArchiveFileName(PathUtils
                                        .getFileName(archivePath)))
                                : targetFolder);
        
        LOG.debug("Invoke command: {}", command);
        
        StringBuilder output = new StringBuilder();
        
        SSHCommandRunner sshCommandRunner = new SSHCommandRunner()
                .withCommand(command)
                .withStdoutAppendable(output)
                .withStopper(stopFlag);
        
        client.exec(sshCommandRunner);
        
        boolean ret = sshCommandRunner.getResult() == 0;
        
        LOG.debug("Output: {}", output);
        
        return ret;
    }

    public boolean createArchive(SSHConnectionHandler client, List<String> files, String targetFolder, IStopper stopFlag) throws RemoteOperationException, OperationCancelledException, SessionClosedException, InterruptedException {
        String text = files.size() > 1 ? PathUtils.getFileName(targetFolder)
                : files.get(0);
        JTextField txtFileName = new JTextField(text);
        JTextField txtTargetFolder = new JTextField(targetFolder);
        JComboBox<String> comboBox = new JComboBox<>(
                compressCommands.keySet().toArray(new String[0]));
        // TODO i18n
        if (JOptionPane.showOptionDialog(null,
                new Object[]{"Archive name", txtFileName, "Target folder",
                        txtTargetFolder, "Archive type", comboBox},
                "Create archive", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.OK_OPTION) {

            StringBuilder sb = new StringBuilder();
            for (String s : files) {
                sb.append(" \"").append(s).append("\"");
            }

            String ext = comboBox.getSelectedItem() + "";

            String compressCmd = String.format(compressCommands.get(ext),
                    sb,
                    PathUtils.combineUnix(txtTargetFolder.getText(),
                            txtFileName.getText() + "." + ext));
            
            String cd = String.format("cd \"%s\";", txtTargetFolder.getText());
            
            LOG.debug("Invoke command: {}", cd + compressCmd);
            
            StringBuilder output = new StringBuilder();
            
            SSHCommandRunner sshCommandRunner = new SSHCommandRunner()
                    .withCommand(cd + compressCmd)
                    .withStdoutAppendable(output)
                    .withStopper(stopFlag);
            
            client.exec(sshCommandRunner);
            
            boolean ret = sshCommandRunner.getResult() == 0;
            
            LOG.debug("Output: {}", output);
            
            return ret;
        }
        return true;
    }
}
