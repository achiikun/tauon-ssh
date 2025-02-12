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
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
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
//            this.wr = new SshClient2(this.info, App.getInputBlocker(), sessionContentPanel);
//            this.wr.connect();
            // Let's use the same connection and open a session
            this.session = sshConnectionHandler.getSession(false);
            this.session.setAutoExpand(true);

            this.session.allocatePTY(
                    SettingsConfigManager.getSettings().getTerminalType(),
                    SettingsConfigManager.getSettings().getTermWidth(),
                    SettingsConfigManager.getSettings().getTermHeight(),
                    0,
                    0,
                    Collections.emptyMap()
            );
            
//            this.channel.reqX11Forwarding("MIT-MAGIC-COOKIE-1", "b0956167c9ad8f34c8a2788878307dc9", 0);

            try{
                this.session.setEnvVar("LANG", "en_US.UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Cannot set environment variable Lang: " + e.getMessage());
            }


            this.shell = (SessionChannel) this.session.startShell();

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
            this.session.close();
//            wr.disconnect();
        } catch (Exception e) {
        }
    }

    @Override
    public void resize(Dimension termSize, Dimension pixelSize) {
        myPendingTermSize = termSize;
        myPendingPixelSize = pixelSize;
        if (session != null) {
            resizeImmediately();
        }

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
        return session != null && session.isOpen() && isInitiated.get();
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

    public boolean isBusy() {
        return session.isOpen();
    }

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
        if (myPendingTermSize != null && myPendingPixelSize != null) {
            setPtySize(shell, myPendingTermSize.width, myPendingTermSize.height, myPendingPixelSize.width,
                    myPendingPixelSize.height);
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
