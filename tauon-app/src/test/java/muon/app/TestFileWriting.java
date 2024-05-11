package muon.app;

import org.newsclub.net.unix.AFSocketAddress;
import tauon.app.util.ssh.SshUtil;

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileWriting {
    
    public static void main(String[] args) throws IOException {
        
        Path socketPath = Path
                .of(new File(".").getAbsolutePath())
                .resolve("writetest.socket");
        Files.deleteIfExists(socketPath);
        
        new Thread(() -> {
            
            try {
                FileWriter socket = new FileWriter(socketPath.toFile());
                
                int i = 0;
                while (i < 10){
                    try {
                        socket.write(i + " ");
                        socket.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    i++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                socket.close();
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        }).start();
        
        File file = socketPath.toFile();
        
        new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream(file);
                
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
