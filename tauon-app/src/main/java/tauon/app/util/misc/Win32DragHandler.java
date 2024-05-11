package tauon.app.util.misc;

import com.sun.jna.platform.FileMonitor;
import com.sun.jna.platform.win32.W32FileMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.updater.UpdateChecker;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public final class Win32DragHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Win32DragHandler.class);
    
    private final FileMonitor fileMonitor = new W32FileMonitor();

    public synchronized void listenForDrop(String keyToListen, Consumer<File> callback) {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        for (File drive : File.listRoots()) {
            if (fsv.isDrive(drive)) {
                try {
                    System.out.println("Adding to watch: " + drive.getAbsolutePath());
                    fileMonitor.addWatch(drive, W32FileMonitor.FILE_RENAMED | W32FileMonitor.FILE_CREATED, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        fileMonitor.addFileListener(e -> {
            File file = e.getFile();
            System.err.println(file);
            if (file.getName().startsWith(keyToListen)) {
                callback.accept(file);
            }
        });
    }

    public synchronized void dispose() {
        System.out.println("File watcher disposed");
        this.fileMonitor.dispose();
    }
}
