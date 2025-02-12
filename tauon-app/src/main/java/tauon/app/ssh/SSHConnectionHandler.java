/**
 *
 */
package tauon.app.ssh;

import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.settings.SiteInfo;
import tauon.app.ssh.filesystem.SshFileSystem;
import tauon.app.ui.containers.main.GraphicalHostKeyVerifier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO don't implement SSHCommandRunner.SudoPasswordPrompter,
 * let it do to GuiHandler and pass it through another parameter in ISSHOperator
 *
 * @author achi
 *
 */
public class SSHConnectionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(SSHConnectionHandler.class);
    
    private final SiteInfo info;
    private final GuiHandle.Delegate guiHandle;
    
    private final TauonSSHClient mainSsh;
    
    private final AtomicBoolean mainClosed = new AtomicBoolean(false);
    
    private final Set<SessionHandle> openSessions = new HashSet<>();
    
    private final LinkedList<TempSshFileSystem> availableFileSystems = new LinkedList<>();
    private final LinkedList<TempSshFileSystem> usingFileSystems = new LinkedList<>();
    
    private final Lock fileSystemsClosingLock = new ReentrantLock();
    private final Condition fileSystemsClosingLockCondition = fileSystemsClosingLock.newCondition();
    
    private final ExecutorService executorService;
    
    public SSHConnectionHandler(
            SiteInfo info,
            GuiHandle guiHandle,
            PasswordFinder passwordFinder,
            GraphicalHostKeyVerifier hostKeyVerifier
    ) {
        
        GuiHandle.Delegate guiHandleDelegate = new GuiHandle.Delegate(guiHandle) {
            @Override
            public BlockHandle blockUi(Object client, UserCancelHandle userCancellable) {
                return guiHandle.blockUi(SSHConnectionHandler.this, userCancellable);
            }
        };
        
        this.info = info;
        this.guiHandle = guiHandleDelegate;
        
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        AtomicLong count = new AtomicLong(0);
        
        ThreadFactory namedThreadFactory = runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setName(info.getName() + "[" + count.getAndIncrement() + "]");
            return thread;
        };
        
        this.executorService = Executors.newCachedThreadPool(namedThreadFactory);
        
        this.mainSsh = new TauonSSHClient(info, guiHandle, passwordFinder, executorService, true, hostKeyVerifier);
        
    }
    
    private TauonSSHClient ensureConnected() throws OperationCancelledException, SessionClosedException, InterruptedException {
        return ensureConnected(false);
    }
    
    public synchronized TauonSSHClient ensureConnected(boolean force) throws OperationCancelledException, SessionClosedException, InterruptedException {
        checkNotClosed();
        
        if(force || !mainSsh.isConnected()){
            checkNotClosed();
            
            while(!mainSsh.connect(force)){
                // Ask user
                
                if (guiHandle.promptReconnect(info.getName(), info.getHost())) {
                    throw new OperationCancelledException();
                }
                
            }
            
        }
        
        return mainSsh;
        
    }
    public synchronized void close() throws InterruptedException, IOException {
        
        if(mainClosed.getAndSet(true))
            return;
        
        for (SessionHandle session: openSessions){
            session.closeSessionSync();
        }
        openSessions.clear();
        
        fileSystemsClosingLock.lock();
        try {
            for (TempSshFileSystem tempSshFileSystem: availableFileSystems){
                tempSshFileSystem.closeSync();
            }
            availableFileSystems.clear();
            
            TempSshFileSystem t;
            while ((t = usingFileSystems.poll()) != null) {
                while(!t.disposed.get())
                    fileSystemsClosingLockCondition.await();
            }
        }finally {
            fileSystemsClosingLock.unlock();
        }
        
        this.mainSsh.close();
        List<Runnable> l = executorService.shutdownNow();
        if(!l.isEmpty())
            LOG.warn("There were {} tasks without being run when the connection was closed", l.size());
        
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        
    }
    
    public void exec(SSHCommandRunner commandRunner) throws SessionClosedException, OperationCancelledException, RemoteOperationException, InterruptedException {
        TauonSSHClient sshClient = ensureConnected();
        try (Session session = sshClient.openSession()) {
            commandRunner.run(session, executorService);
        } catch (IOException e) {
            LOG.error("Exception while operating with temporary session.", e);
            throw new RemoteOperationException.RealIOException(e);
        }
    }
    
    private void checkNotClosed() throws SessionClosedException {
        if(mainClosed.get()){
            throw new SessionClosedException();
        }
    }
    
    public synchronized SessionHandle openSessionHandle() throws SessionClosedException {
        checkNotClosed();
        SessionHandle sessionHandle = new SessionHandle();
        openSessions.add(sessionHandle);
        return sessionHandle;
    }
    
    public SshFileSystem getSshFileSystem() {
        return new SshFileSystem(force -> SSHConnectionHandler.this.ensureConnected(force).getSftpClient());
    }
    
    public SiteInfo getInfo() {
        return info;
    }
    
    public int exec(String command) throws OperationCancelledException, RemoteOperationException, InterruptedException, SessionClosedException {
        SSHCommandRunner sshCommandRunner = new SSHCommandRunner().withCommand(command);
        exec(sshCommandRunner);
        return sshCommandRunner.getResult();
    }
    
    public TempSshFileSystem openTempSshFileSystem() throws SessionClosedException {
        checkNotClosed();
        
        fileSystemsClosingLock.lock();
        try {
            TempSshFileSystem fs = availableFileSystems.poll();
            if (fs == null) {
                fs = new TempSshFileSystem(new AtomicReference<>());
            } else {
                fs.undispose();
            }
            usingFileSystems.add(fs);
            return fs;
        }finally {
            fileSystemsClosingLock.unlock();
        }
    }
    
    public class SessionHandle {
        
        private Session session;
        
        private SessionHandle() {
        
        }
        
        public synchronized Session getSession(boolean force) throws OperationCancelledException, InterruptedException, SessionClosedException, RemoteOperationException {
            
            if(force && session != null){
                try {
                    this.session.close();
                } catch (TransportException | ConnectionException e) {
                    throw new RemoteOperationException.RealIOException(e);
                } finally {
                    this.session = null;
                }
            }
            
            if(session == null){
                this.session = ensureConnected(force).openSession();
            }
            
            return this.session;
        }
        
        public void dispose() {
            // Don't bother if main has been closed because the session will be closed anyway
            if(!mainClosed.get() && openSessions.remove(this))
                closeSessionSync();
        }
        
        private void closeSessionSync() {
            Session session1 = this.session;
            this.session = null;
            if (session1 != null) {
                try {
                    session1.close();
                } catch (TransportException | ConnectionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    public class TempSshFileSystem extends SshFileSystem {
        
        private final AtomicReference<SFTPClient> current;
        private final AtomicBoolean disposed = new AtomicBoolean();
        
        private TempSshFileSystem(AtomicReference<SFTPClient> current) {
            super(new Requester() {
                
                @Override
                public synchronized SFTPClient getSftpClient(boolean force) throws RemoteOperationException, SessionClosedException, OperationCancelledException, InterruptedException {
                    SFTPClient c = current.get();
                    
                    if(force && c != null) {
                        try {
                            c.close();
                        } catch (IOException e) {
                            LOG.error("Failed to close the SFTPClient", e);
                        }
                        c = null;
                    }
                    
                    if(c == null)
                        current.set(c = SSHConnectionHandler.this.ensureConnected(force).newSftpClient());
                    
                    return c;
                }
            });
            this.current = current;
        }
        
        private void undispose(){
            disposed.set(false);
        }
        
        public void dispose() {
            fileSystemsClosingLock.lock();
            try {
                if(!disposed.getAndSet(true)) {
                    if(mainClosed.get()){
                        fileSystemsClosingLockCondition.signalAll();
                    }else {
                        if (!usingFileSystems.remove(this))
                            LOG.error("Just disposed TempSshFileSystem was not present in usingFileSystems list.");
                        availableFileSystems.add(this);
                    }
                }
            }finally {
                fileSystemsClosingLock.unlock();
            }
        }
        
        private void closeSync() {
            SFTPClient c = current.getAndSet(null);
            try {
                c.close();
            } catch (IOException e) {
                LOG.error("Closing a TempSshFileSystem threw an exception.", e);
            }
        }
        
    }
    
}
