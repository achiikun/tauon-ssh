package tauon.app.ssh.filesystem.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.exceptions.AlreadyFailedException;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.exceptions.TauonOperationException;
import tauon.app.ssh.GuiHandle;
import tauon.app.ssh.SSHConnectionHandler;
import tauon.app.ssh.filesystem.*;
import tauon.app.ui.containers.session.pages.files.ssh.SshFileBrowserView;
import tauon.app.util.misc.Constants;

public class FileTransferRemoteToLocal extends FileTransfer{
    private static final Logger LOG = LoggerFactory.getLogger(FileTransferRemoteToLocal.class);
    
    private final SSHConnectionHandler sshConnectionHandler;
    
    public FileTransferRemoteToLocal(
            FileInfo[] files,
            String targetFolder,
            GuiHandle guiHandle,
            SSHConnectionHandler sshConnectionHandler,
            Constants.ConflictAction defaultConflictAction
    ){
        super(guiHandle, files, targetFolder, defaultConflictAction);
        this.sshConnectionHandler = sshConnectionHandler;
    }
    
    public void prepareTransfer(SshFileSystem sourceFs) throws OperationCancelledException, AlreadyFailedException, TauonOperationException, InterruptedException, SessionClosedException {
        prepareTransfer(sourceFs, LocalFileSystem.getInstance());
    }
    
    public void run(){
        SSHConnectionHandler.TempSshFileSystem sourceFs = null;
        LocalFileSystem targetFs = LocalFileSystem.getInstance();
        try {
             sourceFs = sshConnectionHandler.openTempSshFileSystem();
             
             if(progressListener != null)
                progressListener.init(totalSize, filesToTransfer.size());
             
            InputTransferChannel inc = sourceFs.inputTransferChannel();
            OutputTransferChannel outc = targetFs.outputTransferChannel();
            
            for (FileInfoHolder file : filesToTransfer) {
                System.out.println("Copying: " + file.info.getPath());
                
                copyFile(file, targetFs, inc, outc);
                System.out.println("Copying done: " + file.info.getPath());
                incrementProcessedFiles();
            }
            
            if(progressListener != null)
                progressListener.done();
            
        } catch (Exception e) {
            LOG.error("Exception while copying.", e);
            if(progressListener != null)
                progressListener.error(null, e);
        } finally {
            if(sourceFs != null){
                sourceFs.dispose();
            }
        }
    }
    
}
