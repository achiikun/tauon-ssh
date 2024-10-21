/**
 *
 */
package tauon.app.util.externaleditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.SSHRemoteFileInputStream;
import tauon.app.ssh.filesystem.SSHRemoteFileOutputStream;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.externaleditor.FileChangeWatcher.FileModificationInfo;
import tauon.app.util.misc.OptionPaneUtils;
import tauon.app.util.misc.PlatformUtils;
import tauon.app.util.misc.TimeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class ExternalEditorHandler extends JDialog {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExternalEditorHandler.class);
    
    private final JProgressBar progressBar;
    private final JLabel progressLabel;
    private final JFrame frame;
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private FileChangeWatcher fileWatcher;

    
    public ExternalEditorHandler(JFrame frame) {
        super(frame);
        setModal(true);
        setUndecorated(true);
        this.frame = frame;
        setSize(400, 200);

        progressBar = new JProgressBar();
        progressLabel = new JLabel(getBundle().getString("app.external_editor.label.transferring"));
        progressLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        progressLabel.setFont(App.skin.getDefaultFont().deriveFont(18.0f));
        
        JButton btnCancel = new JButton(getBundle().getString("general.action.cancel"));
        Box bottomBox = Box.createHorizontalBox();
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(btnCancel);

        progressLabel.setAlignmentX(Box.LEFT_ALIGNMENT);
        progressBar.setAlignmentX(Box.LEFT_ALIGNMENT);
        bottomBox.setAlignmentX(Box.LEFT_ALIGNMENT);

        Box box = Box.createVerticalBox();
        box.add(progressLabel);
        box.add(progressBar);
        box.add(Box.createVerticalGlue());
        box.add(bottomBox);

        box.setBorder(new EmptyBorder(10, 10, 10, 10));

        this.add(box);
        this.fileWatcher = new FileChangeWatcher(files -> {
            // TODO wait until returning to the app to show this dialog
            List<String> messages = new ArrayList<>();
            // TODO i18n
            messages.add("Some file(s) have been modified, upload changes to server?\n");
            messages.add("Changed file(s):");
            messages.addAll(files.stream().map(FileModificationInfo::toString).collect(Collectors.toList()));
            if (OptionPaneUtils.showOptionDialog(this.frame, messages.toArray(new String[0]),
                    "File changed") == JOptionPane.OK_OPTION) {
                this.fileWatcher.stopWatching();
                App.EXECUTOR.submit(() -> {
                    try {
                        this.saveRemoteFiles(files);
                    } catch (Exception e) {
                        LOG.error("Error while saving remote files.", e);
                    }
                });
            }
        }, 1000);

        this.fileWatcher.startWatching();

    }

    private void saveRemoteFiles(List<FileModificationInfo> files) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(0);
            setVisible(true);
        });
        
        this.fileWatcher.stopWatching();
        try {
            LOG.trace("Start transferring files.");
            
            long totalSize = 0L;
            for (FileModificationInfo info : files) {
                totalSize += info.localFile.length();
            }
            
            LOG.debug("Total byte to transfer {}", totalSize);
            
            long totalBytes = 0L;
            for (FileModificationInfo info : files) {
                try {
                    totalBytes += saveRemoteFile(info, totalSize, totalBytes);
                } catch (Exception e) {
                    LOG.error("Error while transfering file.", e);
                }
            }
            
            LOG.trace("Transferring complete.");
            
        }finally {
            fileWatcher.resumeWatching();
            SwingUtilities.invokeLater(() -> {
                setVisible(false);
            });
        }
    }

    /**
     * @param info
     * @param total
     * @param totalBytes
     * @return
     */
    private Long saveRemoteFile(FileModificationInfo info, long total, long totalBytes) throws Exception {
        LOG.trace("Transferring file: {}", info);
        
        SessionContentPanel scp = info.activeSessionId;
        if (scp == null) {
            LOG.warn("No session found. Skipping file.");
            return info.remoteFile.getSize();
        }

        scp.runSSHOperation(instance -> {
            long totalBytes1 = totalBytes;
            LOG.debug("Opening local file: {}; and remote file: {}", info.localFile, info.remoteFile.getPath());
            try (
                    OutputStream out = instance.getSshFs().outputTransferChannel()
                            .getOutputStream(info.remoteFile.getPath());
                    InputStream in = new FileInputStream(info.localFile)
            ) {
                int cap = 8192;
                if (out instanceof SSHRemoteFileOutputStream) {
                    cap = ((SSHRemoteFileOutputStream) out).getBufferCapacity();
                }
                
                byte[] b = new byte[cap];
                while (!this.stopFlag.get()) {
                    int x = in.read(b);
                    if (x == -1) {
                        break;
                    }
                    totalBytes1 += x;
                    out.write(b, 0, x);
                    final int progress = total > 0 ? (int) ((totalBytes1 * 100) / total) : 100;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                    });
                }
            }
        });
        
        LOG.trace("Transferring file complete.");
        
        return info.remoteFile.getSize();
    }

    /**
     * Downloads a remote file using SFTP in a temporary directory and if download
     * completes successfully, adds it for monitoring.
     *
     * @param remoteFile
     * @param remoteFs
     * @param activeSessionId
     * @param openWith        should show windows open with dialog
     * @param app             should open with specified app
     * @throws IOException
     */
    public void openRemoteFile(FileInfo remoteFile, SshFileSystem remoteFs, SessionContentPanel activeSessionId, boolean openWith,
                               String app) throws IOException {
        this.fileWatcher.stopWatching();
        Path tempFolderPath = Files.createTempDirectory(UUID.randomUUID().toString());
        Path localFilePath = tempFolderPath.resolve(remoteFile.getName());
        this.stopFlag.set(false);
        this.progressLabel.setText(remoteFile.getName());
        this.progressBar.setValue(0);
        File localFile = localFilePath.toFile();
        
        App.EXECUTOR.submit(() -> {
            SwingUtilities.invokeLater(this::repaint);
            try (InputStream in = remoteFs.inputTransferChannel().getInputStream(remoteFile.getPath());
                 OutputStream out = new FileOutputStream(localFile)) {
                int cap = 8192 * 8;
                if (in instanceof SSHRemoteFileInputStream) {
                    cap = ((SSHRemoteFileInputStream) in).getBufferCapacity();
                }
                byte[] b = new byte[cap];
                long totalBytes = 0L;
                while (!this.stopFlag.get()) {
                    int x = in.read(b);
                    if (x == -1) {
                        break;
                    }
                    totalBytes += x;
                    out.write(b, 0, x);
                    final int progress = remoteFile.getSize() > 0 ? (int) ((totalBytes * 100) / remoteFile.getSize()) : 100;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                    });
                }
                if(!localFile.setLastModified(TimeUtils.toEpochMilli(remoteFile.getLastModified())))
                    LOG.warn("Could not set last modified on the local file.");
                    
                fileWatcher.addForMonitoring(remoteFile, localFilePath.toAbsolutePath().toString(), activeSessionId);
            } catch (Exception e) {
                LOG.error("Error while copying remote file.", e);
            } finally {
                fileWatcher.resumeWatching();
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (app == null) {
                            PlatformUtils.openWithDefaultApp(localFilePath.toFile(), openWith);
                        } else {
                            PlatformUtils.openWithApp(localFilePath.toFile(), app);
                        }
                    } catch (Exception e) {
                        LOG.error("Error while opening file with default app.", e);
                    }
                    setVisible(false);
                });
            }
        });
        
        setLocationRelativeTo(frame);
        revalidate();
        repaint();
        setVisible(true);
    
    }
}
