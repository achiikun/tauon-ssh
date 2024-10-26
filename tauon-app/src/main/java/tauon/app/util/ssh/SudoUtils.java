package tauon.app.util.ssh;

import net.schmizz.sshj.connection.channel.direct.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.ssh.TauonRemoteSessionInstance;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class SudoUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SudoUtils.class);
    
    private static final JPasswordField PASSWORD_FIELD = new JPasswordField(30);

//    public static int runSudo(String command, TauonRemoteSessionInstance instance, String password) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
//        String prompt = UUID.randomUUID().toString();
//        AtomicBoolean firstTime = new AtomicBoolean(true);
//        String fullCommand = "sudo -S -p '" + prompt + "' " + command;
//        LOG.debug("Going to run: {}", fullCommand);
//        return instance.exec(fullCommand, cmd -> {
//            try {
//                InputStream in = cmd.getInputStream();
//                OutputStream out = cmd.getOutputStream();
//                StringBuilder sb = new StringBuilder();
//                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
//
//                char[] b = new char[8192];
//                while (cmd.isOpen()) {
//                    int x = r.read(b);
//                    if (x > 0) {
//                        sb.append(b, 0, x);
//                    }
//
//                    if (sb.indexOf(prompt) != -1) {
//                        if (firstTime.get() || JOptionPane.showOptionDialog(null,
//                                new Object[]{"User password", PASSWORD_FIELD},
//                                "Authentication",
//                                JOptionPane.OK_CANCEL_OPTION,
//                                JOptionPane.PLAIN_MESSAGE, null, null,
//                                null) == JOptionPane.OK_OPTION
//                        ) {
//                            if (firstTime.get()) {
//                                firstTime.set(false);
//                                PASSWORD_FIELD.setText(password);
//
//                            }
//                            sb = new StringBuilder();
//                            out.write((new String(PASSWORD_FIELD.getPassword()) + "\n").getBytes());
//                            out.flush();
//                        } else {
//                            cmd.close();
//                            return -2;
//                        }
//                    }
//                    Thread.sleep(50);
//                }
//                cmd.join();
//                cmd.close();
//                return cmd.getExitStatus();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return -1;
//            }
//        }, true);
//    }
    
    public static int runSudo(String command, TauonRemoteSessionInstance instance) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String prompt = UUID.randomUUID().toString();
        String fullCommand = "sudo -S -p '" + prompt + "' " + command;
        LOG.debug("Going to run: {}", fullCommand);
        return instance.exec(fullCommand, cmd -> {
            InputStream in = cmd.getInputStream();
            OutputStream out = cmd.getOutputStream();
            StringBuilder sb = new StringBuilder();
            Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
            
            char[] b = new char[8192];
            
            while (cmd.isOpen()) {
                
                int n = r.read(b);
                if (n > 0) {
                    sb.append(b, 0, n);
                }
                
                if (sb.indexOf(prompt) != -1) {
                    // Reset input buffer to find another PROMPT
                    sb = new StringBuilder();
                    char[] sudo = instance.getGuiHandle().getSUDOPassword(false);
                    out.write((new String(sudo) + "\n").getBytes());
                    out.flush();
                }
                
                // Wait 50 millis or until Command is joined
                cmd.join(50, TimeUnit.MILLISECONDS);
            }
            cmd.join();
            cmd.close();
            return cmd.getExitStatus();
        }, true);
    }
    
    public static int runSudoWithOutput(
            String exports,
            String command,
            AtomicBoolean stopFlag,
            TauonRemoteSessionInstance instance,
            StringBuilder output,
            StringBuilder error
    ) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String prompt = UUID.randomUUID().toString();
        String fullCommand = exports + "sudo -E -S -p '" + prompt + "' " + command;
        LOG.debug("Going to run with exports and output: {}", fullCommand);
        return instance.exec(fullCommand, cmd -> handleCommandWithSudo(instance, cmd, prompt, stopFlag, output, error), true);
    }
    
    public static int runSudoWithOutput(
            String command,
            AtomicBoolean stopFlag,
            TauonRemoteSessionInstance instance,
            StringBuilder output,
            StringBuilder error
    ) throws RemoteOperationException, OperationCancelledException, SessionClosedException {
        String prompt = UUID.randomUUID().toString();
        String fullCommand = "sudo -S -p '" + prompt + "' " + command;
        LOG.debug("Going to run with output: {}", fullCommand);
        return instance.exec(fullCommand, cmd -> handleCommandWithSudo(instance, cmd, prompt, stopFlag, output, error), true);
    }
    
    private static int handleCommandWithSudo(TauonRemoteSessionInstance instance, Session.Command cmd, String prompt, AtomicBoolean stopFlag, StringBuilder output, StringBuilder error) throws OperationCancelledException, IOException {
        InputStream in = cmd.getInputStream();
        OutputStream out = cmd.getOutputStream();
        StringBuilder sb = new StringBuilder();

//                    BufferedOutputStream toStdOut = new BufferedOutputStream(System.out);
        Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
        
        char[] b = new char[8192];
        
        int n;
        while ((n = r.read(b)) != -1) {
            
            if(stopFlag != null && stopFlag.get())
                throw new OperationCancelledException();
            
//            if (n == 0) {
//                sb.append(b, 0, n);
//            }

//            int ch = r.read();
//            if (ch == -1)
//                break;
            
            if (sb != null)
                sb.append(b, 0, n);
//                sb.append((char) ch);
            
            output.append(b, 0, n); // TODO don't send prompt
//                        toStdOut.write((char) ch);
            
            if (sb != null && sb.indexOf(prompt) != -1) {
                sb = null; // Only read first prompt
                char[] sudo = instance.getGuiHandle().getSUDOPassword(false);
                out.write((new String(sudo) + "\n").getBytes());
                out.flush();
            }
            
        }
        
        cmd.join();
        cmd.close();
        return cmd.getExitStatus();
    }
    
}
