package tauon.app.ssh;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Signal;
import org.apache.commons.io.output.AppendableWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSHCommandRunner {
    private static final Logger LOG = LoggerFactory.getLogger(SSHCommandRunner.class);
    
    private HashMap<String, String> envVars;
    
    private String command;
    
    private IStopper stopper;
    
    private Appendable stdoutAppendable;
    private Appendable stderrAppendable;
    
    private StringBuilder stdoutStringBuilder;
    private StringBuilder stderrStringBuilder;
    
    private String stdoutString;
    private String stderrString;
    
    private OutputStream stdoutStream;
    private OutputStream stderrStream;
    
    private int result;
    
    private SudoPasswordPrompter sudo;
    
    public SSHCommandRunner(){
    
    }
    
    public SSHCommandRunner withCommand(String command){
        this.command = command;
        return this;
    }
    
    public SSHCommandRunner withStopper(IStopper stopper){
        this.stopper = stopper;
        return this;
    }
    
    public SSHCommandRunner withSudo(SudoPasswordPrompter sudoPasswordRetriever){
        this.sudo = sudoPasswordRetriever;
        return this;
    }
    
    public SSHCommandRunner withStdoutString(){
        this.stdoutStringBuilder = new StringBuilder();
        return this;
    }
    
    public SSHCommandRunner withStderrString(){
        this.stderrStringBuilder = new StringBuilder();
        return this;
    }
    
    public SSHCommandRunner withStdoutAppendable(Appendable appendable){
        this.stdoutAppendable = appendable;
        return this;
    }
    
    public SSHCommandRunner withStderrAppendable(Appendable appendable){
        this.stderrAppendable = appendable;
        return this;
    }
    
    public SSHCommandRunner withStdoutStream(OutputStream appendable){
        this.stdoutStream = appendable;
        return this;
    }
    
    public SSHCommandRunner withStderrStream(OutputStream appendable){
        this.stderrStream = appendable;
        return this;
    }
    
    public SSHCommandRunner withEnvVar(String var, String value){
        if(this.envVars == null){
            this.envVars = new HashMap<>();
        };
        this.envVars.put(var, value);
        return this;
    }
    
    private boolean hasStderr(){
        return stderrStringBuilder != null || stderrAppendable != null || stderrStream != null;
    }
    
    private boolean hasStdout(){
        return stdoutStringBuilder != null || stdoutAppendable != null || stdoutStream != null;
    }
    
    void run(Session session, ExecutorService executorService) throws OperationCancelledException, RemoteOperationException.RealIOException {
        
        Future<?> stderrFuture = null;
        Future<?> stdoutFuture = null;
        
        AtomicBoolean isCancelled = new AtomicBoolean();
        
        try {
            
            String prompt = UUID.randomUUID().toString();
            byte[] bytePrompt = prompt.getBytes(StandardCharsets.US_ASCII);
            
            StringBuilder fullCommandStringBuilder = new StringBuilder();
            
            session.setAutoExpand(true);
            
            if(envVars != null){
                for(Map.Entry<String, String> e: envVars.entrySet()){
                    // TODO This doesn't work, know why
//                    session.setEnvVar(e.getKey(), e.getValue());
                    
                    fullCommandStringBuilder.append(e.getKey()).append("=").append(e.getValue()).append(";");
                }
                
            }
            
            // https://github.com/hierynomus/sshj/issues/285
            // Why not to use pty with sudo:
            // https://superuser.com/questions/1581768/why-isnt-putty-getting-ssh-msg-channel-extended-data-packets-when-ttys-are-enab
            if (sudo != null) {
                // Sudo NOT needs to allocate a pty, this action provokes the stderr to be mixed with stdout,
                // By setting -S option, the password sending works fine
//                session.allocatePTY("vt100", 80, 24, 0, 0, Collections.emptyMap());
                fullCommandStringBuilder.append("sudo -S").append(envVars != null ? " -E" : "").append(" -p '").append(prompt).append("' ").append(command);
            }else{
                fullCommandStringBuilder.append(command);
            }
            
            String fullCommand = fullCommandStringBuilder.toString();
            try (final Session.Command cmd = session.exec(fullCommand)) {
                LOG.debug("Command and Session started: {}", fullCommand);
                
                if (sudo != null || hasStderr()) {
                    
                    stderrFuture = executorService.submit(() -> {
                        InputStream err = cmd.getErrorStream();
                        ByteBuffer initialStderrByteBuffer;
                        
                        try {
                            
                            if (sudo != null) {
                                
                                OutputStream out = cmd.getOutputStream();
                                
                                initialStderrByteBuffer = ByteBuffer.allocate(prompt.length() * 3);
                                boolean retrying = false;
                                
                                while (cmd.isOpen()) {
                                    
                                    // When the command is closed, this method returns a -1, so, if sudo never asked for a password,
                                    // Stderr will be still read, in the block below (using whatever currently in 'initialStderrByteBuffer').
                                    int n = err.read();
                                    if(n < 0){
                                        break; // Stream was closed
                                    }else if (n > 0) {
                                        initialStderrByteBuffer.put((byte) n);
                                    }
                                    
                                    if (endMatches(initialStderrByteBuffer, bytePrompt)) {
                                        
                                        // Reset input buffer to find another PROMPT
                                        char[] password = sudo.promptSudoPassword(retrying);
                                        if (password == null) {
                                            isCancelled.set(true);
                                            break;
                                        }
                                        retrying = true;
                                        out.write((new String(password) + "\n").getBytes());
                                        out.flush();
                                        
                                        // Reset stringBuilder
                                        initialStderrByteBuffer.position(0);
                                    }
                                    
                                    if (initialStderrByteBuffer.position() >= initialStderrByteBuffer.capacity()) {
                                        // The buffer is full, assume that password went well
                                        break;
                                    }
                                    
                                    // Wait 50 millis or until Command is joined
                                    try {
                                        cmd.join(50, TimeUnit.MILLISECONDS);
                                    }catch (ConnectionException connectionException){
                                        if(!(connectionException.getCause() instanceof TimeoutException)) {
                                            throw connectionException;
                                        }
                                    }
                                    
                                    if (!cmd.isOpen() && stopper.isStopped()) {
                                        isCancelled.set(true);
                                        break;
                                    }
                                    
                                }
                                
                            } else {
                                initialStderrByteBuffer = null;
                            }
                        
                            if (stderrStringBuilder != null) {
                                byte[] bytes = readNBytes(err, Integer.MAX_VALUE, initialStderrByteBuffer);
                                stderrString = new String(bytes, cmd.getRemoteCharset());
                            } else if (stderrAppendable != null) {
                                if(initialStderrByteBuffer != null) {
                                    InputStreamReader bb = new InputStreamReader(new ByteBufferBackedInputStream(initialStderrByteBuffer));
                                    bb.transferTo(new AppendableWriter<>(stderrAppendable));
                                }
                                InputStreamReader isr = new InputStreamReader(err);
                                isr.transferTo(new AppendableWriter<>(stderrAppendable));
                            } else if (stderrStream != null) {
                                if(initialStderrByteBuffer != null) {
                                    byte[] arr = new byte[initialStderrByteBuffer.remaining()];
                                    initialStderrByteBuffer.get(arr);
                                    stderrStream.write(arr);
                                }
                                err.transferTo(stderrStream);
                            }
                            
                        } catch (IOException e) {
                            LOG.error("Exception while reading stderr.", e);
                        } catch (OperationCancelledException e) {
                            isCancelled.set(true);
                            throw new RuntimeException(e);
                        }
                    });
                    
                }
                
                if (hasStdout()){
                    InputStream in = cmd.getInputStream();
                    stdoutFuture = executorService.submit(() -> {
                        try {
                            if (stdoutStringBuilder != null) {
                                // TODO user StringBuffer and InputStreamReader
                                byte[] bytes = in.readAllBytes();
                                stdoutString = new String(bytes, cmd.getRemoteCharset());
                            } else if (stdoutAppendable != null) {
                                InputStreamReader isr = new InputStreamReader(in);
                                isr.transferTo(new AppendableWriter<>(stdoutAppendable));
                            } else if (stdoutStream != null) {
                                in.transferTo(stdoutStream);
                            }
                        } catch (IOException e) {
                            LOG.error("Exception while reading stdout.");
                        }
                    });
                }
                
                if(stopper != null) {
                    while (cmd.isOpen()) {
                        if (stopper.isStopped()) {
                            isCancelled.set(true);
                            break;
                        }
                        try {
                            cmd.join(500, TimeUnit.MILLISECONDS);
                        }catch (ConnectionException connectionException){
                            if(!(connectionException.getCause() instanceof TimeoutException)) {
                                throw connectionException;
                            }
                        }
                    }
                }else {
                    // Wait to finish the command
                    // TODO show users that process is not responding
                    cmd.join();
                }
                
                if(isCancelled.get()){
                    while (cmd.isOpen()) {
                        // https://www.semicolonandsons.com/code_diary/unix/kill-vs-term-vs-int-vs-quit-signals
                        cmd.signal(Signal.TERM);
                        try {
                            cmd.join(5, TimeUnit.SECONDS);
                        }catch (ConnectionException connectionException){
                            if(!(connectionException.getCause() instanceof TimeoutException)) {
                                throw connectionException;
                            }
                        }
                        if (cmd.isOpen()) {
                            cmd.signal(Signal.KILL);
                            try {
                                cmd.join(5, TimeUnit.SECONDS);
                            }catch (ConnectionException connectionException){
                                if(!(connectionException.getCause() instanceof TimeoutException)) {
                                    throw connectionException;
                                }
                            }
                        }
                    }
                }
                
                closeFutures(stdoutFuture, stderrFuture);
                
                LOG.debug("Command and Session closed: isOpen={} isEof={} exitStatus={}", cmd.isOpen(), cmd.isEOF(), cmd.getExitStatus());
                
                result = cmd.getExitStatus();
                
                if (isCancelled.get()) {
                    throw new OperationCancelledException();
                }
                
            } catch (IOException e) {
                throw new RemoteOperationException.RealIOException(e);
            }
            
//        } catch (IOException e) {
//            throw new RemoteOperationException.RealIOException(e);
        } finally {
            closeFutures(stdoutFuture, stderrFuture);
        }
        
    }
    
    // TODO show users that process is not responding
    private static void closeFutures(Future<?> stdoutFuture, Future<?> stderrFuture) {
        if (stdoutFuture != null) {
            try {
                stdoutFuture.get(15, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.error("Timeout while waiting the stdout reading to join.");
            } catch (Exception ignore) {
            
            }
        }
        
        if (stderrFuture != null) {
            try {
                stderrFuture.get(15, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.error("Timeout while waiting the stderr reading to join.");
            } catch (Exception ignore) {
            
            }
        }
    }
    
    private boolean endMatches(ByteBuffer sb, byte[] bytePrompt) {
        if(sb.position() < bytePrompt.length)
            return false;
        int base = sb.position() - bytePrompt.length;
        for (int i = 0; i < bytePrompt.length; i++) {
            if(sb.get(base + i) != bytePrompt[i])
                return false;
        }
        return true;
    }
    
    public byte[] readNBytes(InputStream is, int len, ByteBuffer initial) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        } else {
            List<byte[]> bufs = null;
            byte[] result = null;
            int total = 0;
            int remaining = len;
            
            int n;
            do {
                byte[] buf = new byte[Math.min(remaining, 16384)];
                
                int nread = 0;
                if(initial != null){
                    int initialRemaining = initial.remaining();
                    if(buf.length > initialRemaining){
                        initial.get(buf, 0, initialRemaining);
                        total = nread = initialRemaining;
                    }else{
                        bufs = new ArrayList();
                        byte[] arr = new byte[initial.remaining()];
                        initial.get(arr);
                        bufs.add(arr);
                        total = initialRemaining;
                    }
                    initial = null;
                }
                
                for(; (n = is.read(buf, nread, Math.min(buf.length - nread, remaining))) > 0; remaining -= n) {
                    nread += n;
                }
                
                if (nread > 0) {
                    if (2147483639 - total < nread) {
                        throw new OutOfMemoryError("Required array size too large");
                    }
                    
                    if (nread < buf.length) {
                        buf = Arrays.copyOfRange(buf, 0, nread);
                    }
                    
                    total += nread;
                    if (result == null) {
                        result = buf;
                    } else {
                        if (bufs == null) {
                            bufs = new ArrayList();
                            bufs.add(result);
                        }
                        
                        bufs.add(buf);
                    }
                }
            } while(n >= 0 && remaining > 0);
            
            if (bufs == null) {
                if (result == null) {
                    return new byte[0];
                } else {
                    return result.length == total ? result : Arrays.copyOf(result, total);
                }
            } else {
                result = new byte[total];
                int offset = 0;
                remaining = total;
                
                for(byte[] b : bufs) {
                    int count = Math.min(b.length, remaining);
                    System.arraycopy(b, 0, result, offset, count);
                    offset += count;
                    remaining -= count;
                }
                
                return result;
            }
        }
    }
    
    public int getResult(){
        return result;
    }
    
    public String getStdoutString(){
        return stdoutString;
    }
    
    public String getStderrString(){
        return stderrString;
    }
    
    public interface SudoPasswordPrompter{
        char[] promptSudoPassword(boolean isRetrying) throws OperationCancelledException;
    }
    
}
