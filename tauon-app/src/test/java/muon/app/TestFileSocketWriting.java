package muon.app;

import org.newsclub.net.unix.AFSocketAddress;
import tauon.app.util.ssh.SshUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileSocketWriting {
    
    public static void main(String[] args) throws IOException {
        
        Path socketPath = Path
                .of(new File("").getAbsolutePath())
                .resolve("writetest.socket");
        Files.deleteIfExists(socketPath);
        
        AFSocketAddress afSocketAddress = (AFSocketAddress) SshUtil.socketAddress(socketPath.toString());
        
        ServerSocket serverSocket = ((AFSocketAddress) afSocketAddress).getAddressFamily().newServerSocket();
        serverSocket.bind(afSocketAddress);
        
        new Thread(() -> {
            
            try {
                Socket socket = serverSocket.accept();
                
                OutputStream outputStream = socket.getOutputStream();
                
                int i = 0;
                while (i < 10){
                    outputStream.write((i + " ").getBytes(StandardCharsets.UTF_8));
                    i++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                socket.close();
                serverSocket.close();
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        }).start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        File file = socketPath.toFile();
        
        new Thread(() -> {
            try {
                Socket socket = ((AFSocketAddress) afSocketAddress).getAddressFamily().newSocket();
                socket.connect(afSocketAddress);
                
                InputStream fis = socket.getInputStream();//new FileInputStream(file);
                
                int by;
                while ((by = fis.read()) != -1){
                    System.out.print((char) by);
                }
                System.out.println();
                
                System.out.println("Reading has finished");
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        }).start();
        
    }
    
    
    
}
