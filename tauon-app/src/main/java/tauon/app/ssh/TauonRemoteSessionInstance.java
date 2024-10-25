/**
 *
 */
package tauon.app.ssh;

import com.jcraft.jsch.IO;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.containers.main.GraphicalHostKeyVerifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author subhro
 *
 */
public class TauonRemoteSessionInstance {
    
    private static final Logger LOG = LoggerFactory.getLogger(TauonRemoteSessionInstance.class);
    
    private final TauonSSHClient ssh;
    private final SshFileSystem sshFs;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final GuiHandle.Delegate<TauonSSHClient> guiHandle;
    private final SessionInfo info;
    
    public TauonRemoteSessionInstance(SessionInfo info, GuiHandle<TauonRemoteSessionInstance> guiHandle, PasswordFinder passwordFinder, boolean openPortForwarding, GraphicalHostKeyVerifier hostKeyVerifier) {
        
        GuiHandle.Delegate<TauonSSHClient> guiHandleDelegate = new GuiHandle.Delegate<>(guiHandle) {
            @Override
            public BlockHandle blockUi(TauonSSHClient client, UserCancelHandle userCancellable) {
                return guiHandle.blockUi(TauonRemoteSessionInstance.this, userCancellable);
            }
        };
        
        this.info = info;
        this.guiHandle = guiHandleDelegate;
        this.ssh = new TauonSSHClient(info, guiHandleDelegate, passwordFinder, executorService, openPortForwarding, hostKeyVerifier);
        this.sshFs = new SshFileSystem(this);
    }
    
    public GuiHandle.Delegate<TauonSSHClient> getGuiHandle() {
        return guiHandle;
    }
    
    public void ensureConnected() throws OperationCancelledException, SessionClosedException, InterruptedException {
        ensureConnected(false);
    }
    
    public void ensureConnected(boolean force) throws OperationCancelledException, SessionClosedException, InterruptedException {
        
        if(force || !isConnected()){
            
            if(closed.get())
                throw new SessionClosedException();
            
            while(!ssh.connect(force)){
                // Ask user
                
                if (guiHandle.promptReconnect(info.getName(), info.getHost())) {
                    throw new OperationCancelledException();
                }
                
            }
            
        }
        
    }
    
    private interface Operator{
        int operate(Session session) throws IOException, OperationCancelledException, RemoteOperationException;
    }
    
    private int getSession(Operator operator) throws RemoteOperationException, SessionClosedException, OperationCancelledException {
        try (Session session = ssh.openSession()) {
            return operator.operate(session);
        } catch (IOException e) {
            throw new RemoteOperationException.RealIOException(e);
        }
    }
    
    public interface ExecCallback{
        int operate(Command command) throws IOException, RemoteOperationException, OperationCancelledException;
    }
    
    public int exec(String command, ExecCallback callback, boolean pty) throws RemoteOperationException, SessionClosedException, OperationCancelledException {
        
        return getSession(session -> {
            session.setAutoExpand(true);
            if (pty) {
                session.allocatePTY("vt100", 80, 24, 0, 0, Collections.emptyMap());
            }
            try (final Command cmd = session.exec(command)) {
                return callback.operate(cmd);
            }
        });

    }

    public int exec(String command, AtomicBoolean stopFlag)
            throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        return exec(command, stopFlag, null, null);
    }

    public int exec(String command, AtomicBoolean stopFlag, StringBuilder output)
            throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        return exec(command, stopFlag, output, null);
    }

    public int exec(String command, AtomicBoolean stopFlag, StringBuilder output, StringBuilder error)
            throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        ByteArrayOutputStream bout = output == null ? null : new ByteArrayOutputStream();
        ByteArrayOutputStream berr = error == null ? null : new ByteArrayOutputStream();
        int ret = execBin(command, stopFlag, bout, berr);
        LOG.debug("Out len: " + (bout != null ? bout.size() : 0));
        LOG.debug("Err len: " + (berr != null ? berr.size() : 0));
        if (output != null) {
            output.append(bout.toString(StandardCharsets.UTF_8));
        }
        if (error != null) {
            error.append(berr.toString(StandardCharsets.UTF_8));
        }
        return ret;
    }

    public int execBin(String command, AtomicBoolean stopFlag, OutputStream bout, OutputStream berr) throws RemoteOperationException, SessionClosedException, OperationCancelledException {

        if (stopFlag.get()) { // TODO create a more efficient way to stop a process
            throw new OperationCancelledException();
        }
        
        return getSession(session -> {
            
            session.setAutoExpand(true);
            try (final Command cmd = session.exec(command)) {
                LOG.debug("Command and Session started: {}", command);

                InputStream in = cmd.getInputStream();
                InputStream err = cmd.getErrorStream();

                byte[] b = new byte[8192];

                do {
                    if (stopFlag.get()) {
                        throw new OperationCancelledException();
                    }

                    if (in.available() > 0) {
                        int m = in.available();
                        while (m > 0) {
                            int x = in.read(b, 0, Math.min(m, b.length));
                            if (x == -1) {
                                break;
                            }
                            m -= x;
                            if (bout != null) {
                                bout.write(b, 0, x);
                            }

                        }
                    }

                    if (err.available() > 0) {
                        int m = err.available();
                        while (m > 0) {
                            int x = err.read(b, 0, Math.min(m, b.length));
                            if (x == -1) {
                                break;
                            }
                            m -= x;
                            if (berr != null) {
                                berr.write(b, 0, x);
                            }

                        }
                    }

                } while (cmd.isOpen() || in.available() > 0 || err.available() > 0);
                
                LOG.debug("Command and Session closed: isOpen={} isEof={} exitStatus={}", cmd.isOpen(), cmd.isEOF(), cmd.getExitStatus());
                
                cmd.close();
                return cmd.getExitStatus();
            }
            
        });
        
    }
    
    /**
     *
     */
    public void close() throws InterruptedException, IOException {
        
        if(closed.getAndSet(true))
            return;
        
        this.sshFs.close();
        this.ssh.close();
        
        executorService.shutdownNow();
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        
    }

    /**
     * @return the sshFs
     */
    public SshFileSystem getSshFs() {
        return sshFs;
    }
    
    public boolean isSessionClosed() {
        return closed.get();
    }
    
    public Session openSession() throws Exception {
        return ssh.openSession();
    }
    
    public SFTPClient getSftpClient() throws RemoteOperationException, SessionClosedException {
        return ssh.getSftpClient();
    }
    
    public boolean isConnected() {
        return ssh.isConnected();
    }
}
