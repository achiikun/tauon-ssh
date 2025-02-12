package tauon.app.ui.containers.session.pages.tools.keys;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.ssh.SSHCommandRunner;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ssh.filesystem.OutputTransferChannel;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.settings.SiteInfo;
import tauon.app.util.misc.PathUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SshKeyManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(SshKeyManager.class);
    
    public static SshKeyHolder getKeyDetails(SessionContentPanel content, SSHConnectionHandler instance) throws OperationCancelledException, TauonOperationException, InterruptedException, SessionClosedException {
        SshKeyHolder holder = new SshKeyHolder();
        loadLocalKey(getPubKeyPath(content.getInfo()), holder);
        loadRemoteKeys(holder, instance.getSshFileSystem());
        return holder;
    }

    private static void loadLocalKey(String pubKeyPath, SshKeyHolder holder) {
        try {
            String homeDir = "user.home";
            Path defaultPath = pubKeyPath == null
                    ? Paths.get(System.getProperty(homeDir), ".ssh", "id_rsa.pub").toAbsolutePath()
                    : Paths.get(pubKeyPath);
            byte[] bytes = Files.readAllBytes(defaultPath);
            holder.setLocalPublicKey(new String(bytes, StandardCharsets.UTF_8));
            holder.setLocalPubKeyFile(defaultPath.toString());
        } catch(NoSuchFileException e){
            LOG.warn("Local ssh keys file not found.");
        }catch (Exception e) {
            LOG.error("Error while reading ssh keys.", e);
        }
    }

    private static void loadRemoteKeys(SshKeyHolder holder, SshFileSystem fileSystem) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String path = fileSystem.getHome() + "/.ssh/id_rsa.pub";
        
        try (InputStream in = fileSystem.inputTransferChannel().getInputStream(path)) {
            byte[] bytes = in.readAllBytes();
            out.write(bytes);
        } catch (IOException e) {
            throw new RemoteOperationException.RealIOException(e);
        }
        
        holder.setRemotePubKeyFile(path);
        holder.setRemotePublicKey(out.toString(StandardCharsets.UTF_8));
        
        out = new ByteArrayOutputStream();
        path = fileSystem.getHome() + "/.ssh/authorized_keys";
        try (InputStream in = fileSystem.inputTransferChannel().getInputStream(path)) {
            byte[] bytes = in.readAllBytes();
            out.write(bytes);
        } catch (IOException e) {
            throw new RemoteOperationException.RealIOException(e);
        }
        
        holder.setRemoteAuthorizedKeys(out.toString(StandardCharsets.UTF_8));
    }

    public static void generateKeys(SshKeyHolder holder, SSHConnectionHandler instance, boolean local) throws TauonOperationException, IOException, OperationCancelledException, InterruptedException, SessionClosedException {
        if (holder.getLocalPublicKey() != null && JOptionPane.showConfirmDialog(null,
                "WARNING: This will overwrite the existing SSH key"
                        + "\n\nIf the key was being used to connect to other servers," + "\nconnection will fail."
                        + "\nYou have to reconfigure all the servers"
                        + "\nto use the new key\nDo you still want to continue?",
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }


        JCheckBox chkGenPassPhrase = new JCheckBox("Use passphrase to protect private key (Optional)");
        JPasswordField txtPassPhrase = new JPasswordField(30);
        txtPassPhrase.setEditable(false);
        chkGenPassPhrase.addActionListener(e -> txtPassPhrase.setEditable(chkGenPassPhrase.isSelected()));

        String passPhrase = new String(txtPassPhrase.getPassword());

        if (JOptionPane.showOptionDialog(null, new Object[]{chkGenPassPhrase, "Passphrase", txtPassPhrase},
                "Passphrase", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.YES_OPTION) {
            if (local) {
                generateLocalKeys(holder, passPhrase);
            } else {
                generateRemoteKeys(instance, holder, passPhrase);
            }
        }
    }

    public static void generateLocalKeys(SshKeyHolder holder, String passPhrase) throws IOException, RemoteOperationException {
        Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        Path pubKeyPath = Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa.pub").toAbsolutePath();
        Path keyPath = Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa").toAbsolutePath();
        // TODO get rid of jsch, copy the functionality
        JSch jsch = new JSch();
        KeyPair kpair = null;
        try {
            kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
        } catch (JSchException e) {
            throw new RemoteOperationException(e);
        }
        Files.createDirectories(sshDir);
        if (!passPhrase.isEmpty()) {
            kpair.writePrivateKey(keyPath.toString(), passPhrase.getBytes(StandardCharsets.UTF_8));
        } else {
            kpair.writePrivateKey(keyPath.toString());
        }
        kpair.writePublicKey(pubKeyPath.toString(), System.getProperty("user.name") + "@localcomputer");
        kpair.dispose();
        loadLocalKey(pubKeyPath.toString(), holder);
    }

    public static void generateRemoteKeys(SSHConnectionHandler instance, SshKeyHolder holder, String passPhrase)
            throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException {
        String path1 = "$HOME/.ssh/id_rsa"; // TODO not hardcode this
        String path = path1 + ".pub";

        // Deleting $HOME/.ssh/id_rsa
        instance.getSshFileSystem().deleteFile(path1, false);

        // Deleting $HOME/.ssh/id_rsa.pub
        instance.getSshFileSystem().deleteFile(path, false);
        
        String cmd = "ssh-keygen -q -N \"" + passPhrase + "\" -f \"" + path1 + "\"";
        
        StringBuilder output = new StringBuilder();
        
        SSHCommandRunner sshCommandRunner = new SSHCommandRunner()
                .withCommand(cmd)
                .withStdoutAppendable(output);
        
        instance.exec(sshCommandRunner);
        
        int ret = sshCommandRunner.getResult();
        
        if (ret != 0) {
            throw new RemoteOperationException.ErrorReturnCode(cmd, ret);
        }
        loadRemoteKeys(holder, instance.getSshFileSystem());
    }

    private static String getPubKeyPath(SiteInfo info) {
        if (info.getPrivateKeyFile() != null && !info.getPrivateKeyFile().isEmpty()) {
            String path = PathUtils.combine(
                    PathUtils.getParent(info.getPrivateKeyFile()),
                    PathUtils.getFileName(info.getPrivateKeyFile()) + ".pub",
                    File.separator
            );
            if (new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    public static void saveAuthorizedKeysFile(String authorizedKeys, SshFileSystem fileSystem) throws TauonOperationException, OperationCancelledException, InterruptedException, SessionClosedException, IOException {
        try {
            fileSystem.getInfo(PathUtils.combineUnix(fileSystem.getHome(), ".ssh"));
        } catch (Exception ignored) {
            // Ignoring exception, create folder .ssh in server if not exists
            fileSystem.mkdir(PathUtils.combineUnix(fileSystem.getHome(), ".ssh"));
        }
        OutputTransferChannel otc = fileSystem.outputTransferChannel();
        try (OutputStream out = otc.getOutputStream(PathUtils.combineUnix(fileSystem.getHome(), "/.ssh/authorized_keys"))) {
            out.write(authorizedKeys.getBytes(StandardCharsets.UTF_8));
        }
    }
}
