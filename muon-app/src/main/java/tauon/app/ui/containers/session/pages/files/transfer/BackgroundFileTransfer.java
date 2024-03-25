package tauon.app.ui.containers.session.pages.files.transfer;

import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileSystem;
import tauon.app.ui.containers.main.FileTransferProgress;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.Constants;

public class BackgroundFileTransfer extends FileTransfer {
    
    private final SessionContentPanel session;
    
    public BackgroundFileTransfer(FileSystem sourceFs, FileSystem targetFs, FileInfo[] files, String targetFolder,
                                  FileTransferProgress callback, Constants.ConflictAction defaultConflictAction, TauonRemoteSessionInstance instance, SessionContentPanel session) {
        super(sourceFs, targetFs, files, targetFolder, defaultConflictAction, instance);
        
        this.session = session;
    }

    public SessionContentPanel getSession() {
        return session;
    }

}
