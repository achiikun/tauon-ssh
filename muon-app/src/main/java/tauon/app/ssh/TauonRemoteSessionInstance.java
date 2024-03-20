/**
 *
 */
package tauon.app.ssh;

import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import org.apache.log4j.Logger;
import tauon.app.settings.SessionInfo;
import tauon.app.ssh.filesystem.SshFileSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Deque;
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
    
    private static final Logger LOG = Logger.getLogger(TauonSSHClient.class);
    
    private final TauonSSHClient ssh;
    private final SshFileSystem sshFs;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    public TauonRemoteSessionInstance(SessionInfo info, GuiHandle<TauonRemoteSessionInstance> guiHandle, PasswordFinder passwordFinder, boolean openPortForwarding) {
        
        GuiHandle.Delegate<TauonSSHClient> guiHandleDelagate = new GuiHandle.Delegate<>(guiHandle) {
            @Override
            public BlockHandle blockUi(TauonSSHClient client, UserCancellable userCancellable) {
                return guiHandle.blockUi(TauonRemoteSessionInstance.this, userCancellable);
            }
        };
        
        this.ssh = new TauonSSHClient(info, guiHandleDelagate, passwordFinder, executorService, openPortForwarding);
        this.sshFs = new SshFileSystem(this.ssh);
    }
    
    public void connect() throws InterruptedException {
        
        if(closed.get())
            return;
        
        ssh.connect();
        
    }
    
    public int exec(String command, Function<Command, Integer> callback, boolean pty) throws Exception {
       
        try (Session session = ssh.openSession()) {
            session.setAutoExpand(true);
            if (pty) {
                session.allocatePTY("vt100", 80, 24, 0, 0, Collections.emptyMap());
            }
            try (final Command cmd = session.exec(command)) {
                return callback.apply(cmd);
            }
        }

    }

    public int exec(String command, AtomicBoolean stopFlag) throws Exception {
        return exec(command, stopFlag, null, null);
    }

    public int exec(String command, AtomicBoolean stopFlag, StringBuilder output) throws Exception {
        return exec(command, stopFlag, output, null);
    }

    public int exec(String command, AtomicBoolean stopFlag, StringBuilder output, StringBuilder error)
            throws Exception {
        ByteArrayOutputStream bout = output == null ? null : new ByteArrayOutputStream();
        ByteArrayOutputStream berr = error == null ? null : new ByteArrayOutputStream();
        int ret = execBin(command, stopFlag, bout, berr);
        if (output != null) {
            output.append(bout.toString(StandardCharsets.UTF_8));
        }
        if (error != null) {
            error.append(berr.toString(StandardCharsets.UTF_8));
        }
        return ret;
    }

    public int execBin(String command, AtomicBoolean stopFlag, OutputStream bout, OutputStream berr) throws Exception {

        if (stopFlag.get()) {
            return -1;
        }
        
        try (Session session = ssh.openSession()) {
            session.setAutoExpand(true);
            try (final Command cmd = session.exec(command)) {
                LOG.debug("Command and Session started");

                InputStream in = cmd.getInputStream();
                InputStream err = cmd.getErrorStream();

                byte[] b = new byte[8192];

                do {
                    if (stopFlag.get()) {
                        LOG.debug("stopflag");
                        break;
                    }

                    if (in.available() > 0) {
                        int m = in.available();
                        while (m > 0) {
                            int x = in.read(b, 0, m > b.length ? b.length : m);
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
                            int x = err.read(b, 0, m > b.length ? b.length : m);
                            if (x == -1) {
                                break;
                            }
                            m -= x;
                            if (berr != null) {
                                berr.write(b, 0, x);
                            }

                        }
                    }

                } while (cmd.isOpen());
                
                LOG.debug(cmd.isOpen() + " " + cmd.isEOF() + " " + cmd.getExitStatus());
                
                LOG.debug("Command and Session closed");

                cmd.close();
                return cmd.getExitStatus();
            }
        }
        
    }
    
    /**
     *
     */
    public void close() throws InterruptedException, IOException {
        
        if(!closed.getAndSet(true))
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
    
}
