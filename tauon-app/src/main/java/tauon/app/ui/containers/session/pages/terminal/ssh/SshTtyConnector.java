package tauon.app.ui.containers.session.pages.terminal.ssh;

import com.jediterm.terminal.Questioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.SettingsConfigManager;
import tauon.app.ssh.SSHConnectionHandler;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.connection.channel.direct.SessionChannel;
import net.schmizz.sshj.transport.TransportException;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class SshTtyConnector implements DisposableTtyConnector {
    private static final Logger LOG = LoggerFactory.getLogger(SshTtyConnector.class);
    
    private InputStreamReader myInputStreamReader;
    private InputStream myInputStream = null;
    private OutputStream myOutputStream = null;
    private SessionChannel shell;
    private Session session;
    private final AtomicBoolean isInitiated = new AtomicBoolean(false);
    @Deprecated
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    @Deprecated
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private Dimension myPendingTermSize;
    private Dimension myPendingPixelSize;

    private final String initialCommand;
    private final SSHConnectionHandler.SessionHandle sshConnectionHandler;

    public SshTtyConnector(String initialCommand, SSHConnectionHandler.SessionHandle sshConnectionHandler) {
        this.initialCommand = initialCommand;
        this.sshConnectionHandler = sshConnectionHandler;
    }

    @Override
    public boolean init(Questioner q) {
        try {
            boolean force = false;
            int retry = 0;
            
            while (!isInitialized() && retry <= 1) {
                
                try {
                    
                    // Let's use the same connection and open a session
                    Session session = sshConnectionHandler.getSession(retry > 0);
                    retry++;
                    
                    session.setAutoExpand(true);
                    
                    session.allocatePTY(
                            SettingsConfigManager.getSettings().getTerminalType(),
                            SettingsConfigManager.getSettings().getTermWidth(),
                            SettingsConfigManager.getSettings().getTermHeight(),
                            0,
                            0,
                            Collections.emptyMap()
                    );
                    
                    try {
                        session.setEnvVar("LANG", "en_US.UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Cannot set environment variable Lang: " + e.getMessage());
                    }
                    
                    
                    this.shell = (SessionChannel) session.startShell();
                    
                    myInputStream = shell.getInputStream();
                    myOutputStream = shell.getOutputStream();
                    myInputStreamReader = new InputStreamReader(myInputStream, StandardCharsets.UTF_8);
                    
                    resizeImmediately();
                    System.out.println("Initiated");
                    
                    if (initialCommand != null) {
                        myOutputStream.write((initialCommand + "\n").getBytes(StandardCharsets.UTF_8));
                        myOutputStream.flush();
                    }
                    
                    isInitiated.set(true);
                }catch (TransportException transportException){
                    if(!(transportException.getCause() instanceof SocketException))
                        throw transportException; // else retry it
                }
                
            }

            return true;
        } catch (Exception e) {
            if(!(e instanceof OperationCancelledException))
                LOG.error("Exception while initializing tty connector.", e);
            isInitiated.set(false);
            isCancelled.set(true);
            return false;
        }
    }

    @Override
    public void close() {
        try {
            stopFlag.set(true);
            System.out.println("Terminal wrapper disconnecting");
            this.sshConnectionHandler.closeSessionSync();
            this.shell = null;
        } catch (Exception e) {
        }
    }

    @Override
    public void resize(Dimension termSize, Dimension pixelSize) {
        myPendingTermSize = termSize;
        myPendingPixelSize = pixelSize;
        resizeImmediately();
    }

    @Override
    public String getName() {
        return "Remote";
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return myInputStreamReader.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        myOutputStream.write(bytes);
        myOutputStream.flush();
    }

    @Override
    public boolean isConnected() {
        return sshConnectionHandler.isSessionOpen() && isInitiated.get();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int waitFor() throws InterruptedException {
        System.out.println("Start waiting...");
        while (!isInitiated.get() || isRunning()) {
            System.out.println("waiting");
            Thread.sleep(100); // TODO: remove busy wait
        }
        System.out.println("waiting exit");
        try {
            shell.join();
        } catch (ConnectionException e) {
            // TODO handle exception
            e.printStackTrace();
        }
        return shell.getExitStatus();
    }

    public boolean isRunning() {
        return shell != null && shell.isOpen();
    }

    @Deprecated
    public boolean isBusy() {
        return session.isOpen();
    }
    
    @Deprecated
    public boolean isCancelled() {
        return isCancelled.get();
    }

    public void stop() {
        stopFlag.set(true);
        close();
    }

    public int getExitStatus() {
        if (shell != null) {
            Integer exit = shell.getExitStatus();
            return exit == null ? -1 : exit;
        }
        return -2;
    }

    private void resizeImmediately() {
        if (isRunning() && myPendingTermSize != null && myPendingPixelSize != null) {
            int col = myPendingTermSize.width;
            int row = myPendingTermSize.height;
            int wp = myPendingPixelSize.width;
            int hp = myPendingPixelSize.height;
            
            System.out.println("Exec pty resized:- col: " + col + " row: " + row + " wp: " + wp + " hp: " + hp);
            
            try {
                shell.changeWindowDimensions(col, row, wp, hp);
            } catch (TransportException e) {
                // TODO handle exception
                e.printStackTrace();
            }
            
            myPendingTermSize = null;
            myPendingPixelSize = null;
        }
    }

    private void setPtySize(Shell shell, int col, int row, int wp, int hp) {
        System.out.println("Exec pty resized:- col: " + col + " row: " + row + " wp: " + wp + " hp: " + hp);
        if (shell != null) {
            try {
                shell.changeWindowDimensions(col, row, wp, hp);
            } catch (TransportException e) {
                // TODO handle exception
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitiated.get();
    }

     @Override
      public boolean ready() throws IOException {
        return myInputStreamReader.ready();
      }

}
