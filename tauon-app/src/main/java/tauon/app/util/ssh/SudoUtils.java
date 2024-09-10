package tauon.app.util.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.containers.session.pages.terminal.ssh.SshTtyConnector;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class SudoUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SudoUtils.class);
    
    private static final JPasswordField PASSWORD_FIELD = new JPasswordField(30);

    public static int runSudo(String command, TauonRemoteSessionInstance instance, String password) {
        String prompt = UUID.randomUUID().toString();
        try {
            AtomicBoolean firstTime = new AtomicBoolean(true);
            String fullCommand = "sudo -S -p '" + prompt + "' " + command;
            System.out.println(
                    "Full sudo: " + fullCommand + "\nprompt: " + prompt);
            int ret = instance.exec(fullCommand, cmd -> {
                try {
                    InputStream in = cmd.getInputStream();
                    OutputStream out = cmd.getOutputStream();
                    StringBuilder sb = new StringBuilder();
                    Reader r = new InputStreamReader(in,
                            StandardCharsets.UTF_8);

                    char[] b = new char[8192];

                    while (cmd.isOpen()) {
                        int x = r.read(b);
                        if (x > 0) {
                            sb.append(b, 0, x);
                        }

                        System.out.println("buffer: " + sb);
                        if (sb.indexOf(prompt) != -1) {
                            if (firstTime.get() || JOptionPane.showOptionDialog(null,
                                    new Object[]{"User password",
                                            PASSWORD_FIELD},
                                    "Authentication",
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.PLAIN_MESSAGE, null, null,
                                    null) == JOptionPane.OK_OPTION) {
                                if (firstTime.get()) {
                                    firstTime.set(false);
                                    PASSWORD_FIELD.setText(password);

                                }
                                sb = new StringBuilder();
                                out.write(
                                        (new String(PASSWORD_FIELD.getPassword())
                                                + "\n").getBytes());
                                out.flush();
                            } else {
                                cmd.close();
                                return -2;
                            }
                        }
                        Thread.sleep(50);
                    }
                    cmd.join();
                    cmd.close();
                    return cmd.getExitStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }, true);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int runSudo(String command, TauonRemoteSessionInstance instance) {
        String prompt = UUID.randomUUID().toString();
        try {
            String fullCommand = "sudo -S -p '" + prompt + "' " + command;
            System.out.println(
                    "Full sudo: " + fullCommand + "\nprompt: " + prompt);
            int ret = instance.exec(fullCommand, cmd -> {
                try {
                    InputStream in = cmd.getInputStream();
                    OutputStream out = cmd.getOutputStream();
                    StringBuilder sb = new StringBuilder();
                    Reader r = new InputStreamReader(in,
                            StandardCharsets.UTF_8);

                    char[] b = new char[8192];

                    while (cmd.isOpen()) {
                        int x = r.read(b);
                        if (x > 0) {
                            sb.append(b, 0, x);
                        }

                        System.out.println("buffer: " + sb);
                        if (sb.indexOf(prompt) != -1) {
                            if (JOptionPane.showOptionDialog(null,
                                    new Object[]{"User password",
                                            PASSWORD_FIELD},
                                    "Authentication",
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.PLAIN_MESSAGE, null, null,
                                    null) == JOptionPane.OK_OPTION) {
                                sb = new StringBuilder();
                                out.write(
                                        (new String(PASSWORD_FIELD.getPassword())
                                                + "\n").getBytes());
                                out.flush();
                            } else {
                                cmd.close();
                                return -2;
                            }
                        }
                        Thread.sleep(50);
                    }
                    cmd.join();
                    cmd.close();
                    return cmd.getExitStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }, true);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public static int runSudoWithOutput(String exports, String command,
                                        TauonRemoteSessionInstance instance, StringBuilder output,
                                        StringBuilder error, String password) {
        String prompt = UUID.randomUUID().toString();
        try {
            String fullCommand = exports + "sudo -E -S -p '" + prompt + "' " + command;
            System.out.println(
                    "Full sudo: " + fullCommand + "\nprompt: " + prompt);
            int ret = instance.exec(fullCommand, cmd -> {
                try {
                    InputStream in = cmd.getInputStream();
                    OutputStream out = cmd.getOutputStream();
                    StringBuilder sb = new StringBuilder();
                    
                    BufferedOutputStream toStdOut = new BufferedOutputStream(System.out);
                    
                    Reader r = new InputStreamReader(in,
                            StandardCharsets.UTF_8);
                    
                    while (true) {
                        int ch = r.read();
                        if (ch == -1)
                            break;
                        if(sb != null)
                            sb.append((char) ch);
                        output.append((char) ch); // TODO don't send prompt
                        toStdOut.write((char) ch);

//                        System.out.println("buffer: " + sb);
                        if (sb != null && sb.indexOf(prompt) != -1) {
                            sb = null;
                            out.write(
                                    (password
                                            + "\n").getBytes());
                            out.flush();
                        }
                        
                    }
                    cmd.join();
                    cmd.close();
                    return cmd.getExitStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }, true);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    public static int runSudoWithOutput(String command,
                                        TauonRemoteSessionInstance instance, StringBuilder output,
                                        StringBuilder error, String password) {
        String prompt = UUID.randomUUID().toString();
        try {
            String fullCommand = "sudo -S -p '" + prompt + "' " + command;
            System.out.println(
                    "Full sudo: " + fullCommand + "\nprompt: " + prompt);
            int ret = instance.exec(fullCommand, cmd -> {
                try {
                    InputStream in = cmd.getInputStream();
                    OutputStream out = cmd.getOutputStream();
                    StringBuilder sb = new StringBuilder();
                    
                    BufferedOutputStream toStdOut = new BufferedOutputStream(System.out);
                    
                    Reader r = new InputStreamReader(in,
                            StandardCharsets.UTF_8);

                    while (true) {
                        int ch = r.read();
                        if (ch == -1)
                            break;
                        if(sb != null)
                            sb.append((char) ch);
                        output.append((char) ch); // TODO don't send prompt
                        toStdOut.write((char) ch);
                        
//                        System.out.println("buffer: " + sb);
                        if (sb != null && sb.indexOf(prompt) != -1) {
                            sb = null;
                            out.write(
                                    (password
                                            + "\n").getBytes());
                            out.flush();
                        }

                    }
                    cmd.join();
                    cmd.close();
                    return cmd.getExitStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }, true);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
